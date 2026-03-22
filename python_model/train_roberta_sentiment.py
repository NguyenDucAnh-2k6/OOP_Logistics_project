# python_model/train_roberta_sentiment.py
import pandas as pd
import numpy as np
import torch
from torch.utils.data import DataLoader, Dataset
from sklearn.model_selection import train_test_split
from transformers import RobertaTokenizer, RobertaForSequenceClassification
from sklearn.metrics import classification_report
import os
import json
import optuna
import warnings

warnings.filterwarnings("ignore")
os.environ["TOKENIZERS_PARALLELISM"] = "false"

# 1. Global Configurations
MAX_LEN = 128
BATCH_SIZE = 16 # Keep smaller for RoBERTa to prevent Out-Of-Memory errors
EPOCHS_PER_TRIAL = 5 
FINAL_EPOCHS = 5
TOTAL_TRIALS = 2
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")
MODEL_NAME = "roberta-base"

# 2. PyTorch Dataset Wrapper
class IMDBDataset(Dataset):
    def __init__(self, encodings, labels):
        self.encodings = encodings
        self.labels = labels

    def __getitem__(self, idx):
        item = {key: val[idx].clone().detach() for key, val in self.encodings.items()}
        item['labels'] = torch.tensor(self.labels[idx], dtype=torch.long)
        return item

    def __len__(self):
        return len(self.labels)

# 3. Optuna Objective Function
def objective(trial, train_loader, val_loader):
    lr = trial.suggest_float('lr', 1e-5, 5e-5, log=True)
    weight_decay = trial.suggest_float('weight_decay', 0.01, 0.1)
    
    model = RobertaForSequenceClassification.from_pretrained(MODEL_NAME, num_labels=3).to(DEVICE)
    optimizer = torch.optim.AdamW(model.parameters(), lr=lr, weight_decay=weight_decay)
    scaler = torch.amp.GradScaler() # Mixed precision for speed
    
    print(f"\n--- Starting Trial {trial.number} ---")
    for epoch in range(EPOCHS_PER_TRIAL):
        model.train()
        train_loss = 0.0
        
        for batch in train_loader:
            optimizer.zero_grad()
            inputs = {k: v.to(DEVICE) for k, v in batch.items() if k != 'labels'}
            labels = batch['labels'].to(DEVICE)
            
            with torch.autocast(device_type=DEVICE.type): # Accelerates transformer math
                outputs = model(**inputs, labels=labels)
                loss = outputs.loss
                
            scaler.scale(loss).backward()
            scaler.step(optimizer)
            scaler.update()
            train_loss += loss.item()
            
        avg_train_loss = train_loss / len(train_loader)
        print(f"  -> Trial {trial.number} | Iteration {epoch+1}/{EPOCHS_PER_TRIAL} | Train Loss: {avg_train_loss:.4f}")
            
    # Validation
    model.eval()
    val_loss = 0.0
    with torch.no_grad():
        for batch in val_loader:
            inputs = {k: v.to(DEVICE) for k, v in batch.items() if k != 'labels'}
            labels = batch['labels'].to(DEVICE)
            outputs = model(**inputs, labels=labels)
            val_loss += outputs.loss.item()
            
    avg_val_loss = val_loss / len(val_loader)
    print(f"🟢 Trial {trial.number} Finished | Val Loss: {avg_val_loss:.4f} | lr:{lr:.5f}")
    return avg_val_loss

def train_roberta_pipeline():
    print(f"🚀 Starting Resilient RoBERTa Pipeline on {DEVICE}...")
    
    SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
    DATA_DIR = os.path.join(SCRIPT_DIR, 'data')
    PROCESSED_DIR = os.path.join(DATA_DIR, 'processed')
    MODELS_DIR = os.path.join(SCRIPT_DIR, 'models')
    
    os.makedirs(PROCESSED_DIR, exist_ok=True)
    os.makedirs(MODELS_DIR, exist_ok=True)

    print("Loading RoBERTa Tokenizer...")
    tokenizer = RobertaTokenizer.from_pretrained(MODEL_NAME)

    # Cache paths for RoBERTa dictionaries
    train_enc_path = os.path.join(PROCESSED_DIR, 'roberta_train_enc.pt')
    val_enc_path = os.path.join(PROCESSED_DIR, 'roberta_val_enc.pt')
    test_enc_path = os.path.join(PROCESSED_DIR, 'roberta_test_enc.pt')
    
    train_lbl_path = os.path.join(PROCESSED_DIR, 'roberta_train_lbl.pt')
    val_lbl_path = os.path.join(PROCESSED_DIR, 'roberta_val_lbl.pt')
    test_lbl_path = os.path.join(PROCESSED_DIR, 'roberta_test_lbl.pt')

    # --- CACHING LOGIC ---
    if os.path.exists(train_enc_path) and os.path.exists(test_lbl_path):
        print("⚡ Found cached RoBERTa encodings! Loading directly into memory...")
        train_encodings = torch.load(train_enc_path, weights_only=False)
        y_train = torch.load(train_lbl_path, weights_only=False)
        val_encodings = torch.load(val_enc_path, weights_only=False)
        y_val = torch.load(val_lbl_path, weights_only=False)
        test_encodings = torch.load(test_enc_path, weights_only=False)
        y_test = torch.load(test_lbl_path, weights_only=False)
    else:
        print("No cache found. Loading IMDB Dataset...")
        df = pd.read_csv(os.path.join(DATA_DIR, "IMDB-Dataset.csv"))
        df['label'] = df['sentiment'].map({'negative': 0, 'positive': 2})

        neutral_texts = ["This is an okay movie.", "I have no strong feelings.", "It was average."]
        df_neutral = pd.DataFrame({'review': neutral_texts * 1000, 'label': [1] * 3000})
        df = pd.concat([df, df_neutral], ignore_index=True)

        X_train, X_temp, y_train, y_temp = train_test_split(df['review'].values, df['label'].values, test_size=0.2, random_state=42)
        X_val, X_test, y_val, y_test = train_test_split(X_temp, y_temp, test_size=0.5, random_state=42)

        print("Tokenizing (This will take a moment, but only happens once)...")
        train_encodings = dict(tokenizer(X_train.tolist(), truncation=True, padding='max_length', max_length=MAX_LEN, return_tensors='pt'))
        val_encodings = dict(tokenizer(X_val.tolist(), truncation=True, padding='max_length', max_length=MAX_LEN, return_tensors='pt'))
        test_encodings = dict(tokenizer(X_test.tolist(), truncation=True, padding='max_length', max_length=MAX_LEN, return_tensors='pt'))

        print("💾 Saving tokenized dictionary tensors to data/processed/...")
        torch.save(train_encodings, train_enc_path)
        torch.save(y_train, train_lbl_path)
        torch.save(val_encodings, val_enc_path)
        torch.save(y_val, val_lbl_path)
        torch.save(test_encodings, test_enc_path)
        torch.save(y_test, test_lbl_path)

    # --- DataLoaders ---
    train_dataset = IMDBDataset(train_encodings, y_train)
    val_dataset = IMDBDataset(val_encodings, y_val)
    test_dataset = IMDBDataset(test_encodings, y_test)

    train_loader = DataLoader(train_dataset, batch_size=BATCH_SIZE, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=BATCH_SIZE)
    test_loader = DataLoader(test_dataset, batch_size=BATCH_SIZE)

    # --- 1. OPTUNA PERSISTENCE ---
    print("\n🚀 Connecting to Optuna Database...")
    study_name = "roberta_sentiment_study"
    storage_name = f"sqlite:///{os.path.join(MODELS_DIR, 'optuna_roberta_study.db')}"
    
    study = optuna.create_study(study_name=study_name, storage=storage_name, load_if_exists=True, direction='minimize')
    completed_trials = len(study.trials)
    remaining_trials = max(0, TOTAL_TRIALS - completed_trials)
    
    if remaining_trials > 0:
        print(f"Resuming study: {completed_trials} trials completed. Running {remaining_trials} more...")
        study.optimize(lambda trial: objective(trial, train_loader, val_loader), n_trials=remaining_trials)
    else:
        print(f"All {TOTAL_TRIALS} Optuna trials are already complete!")

    best_params = study.best_params

    # --- 2. FINAL MODEL CHECKPOINTING ---
    print("\nPreparing Final RoBERTa Model...")
    final_model = RobertaForSequenceClassification.from_pretrained(MODEL_NAME, num_labels=3).to(DEVICE)
    optimizer = torch.optim.AdamW(final_model.parameters(), lr=best_params['lr'], weight_decay=best_params['weight_decay'])
    scaler = torch.amp.GradScaler()

    checkpoint_path = os.path.join(MODELS_DIR, 'roberta_checkpoint.pt')
    start_epoch = 0

    if os.path.exists(checkpoint_path):
        print(f"⚡ Found existing training checkpoint! Resuming...")
        checkpoint = torch.load(checkpoint_path, map_location=DEVICE, weights_only=False)
        final_model.load_state_dict(checkpoint['model_state'])
        optimizer.load_state_dict(checkpoint['optimizer_state'])
        scaler.load_state_dict(checkpoint['scaler_state'])
        start_epoch = checkpoint['epoch'] + 1
        print(f"Resuming from Epoch {start_epoch + 1} / {FINAL_EPOCHS}")

    # Training Loop with Saving
    for epoch in range(start_epoch, FINAL_EPOCHS):
        final_model.train()
        total_loss = 0
        for batch_idx, batch in enumerate(train_loader):
            optimizer.zero_grad()
            inputs = {k: v.to(DEVICE) for k, v in batch.items() if k != 'labels'}
            labels = batch['labels'].to(DEVICE)
            
            with torch.autocast(device_type=DEVICE.type):
                outputs = final_model(**inputs, labels=labels)
                loss = outputs.loss
                
            scaler.scale(loss).backward()
            scaler.step(optimizer)
            scaler.update()
            total_loss += loss.item()
            
            if batch_idx % 100 == 0:
                print(f"Epoch {epoch+1}/{FINAL_EPOCHS} | Batch {batch_idx}/{len(train_loader)} | Loss: {loss.item():.4f}")

        torch.save({
            'epoch': epoch,
            'model_state': final_model.state_dict(),
            'optimizer_state': optimizer.state_dict(),
            'scaler_state': scaler.state_dict(),
            'loss': total_loss
        }, checkpoint_path)
        print(f"💾 Checkpoint saved for Epoch {epoch+1}")

    # --- Evaluation ---
    print("\nEvaluating Final RoBERTa on Test Set...")
    final_model.eval()
    all_preds, all_targets = [], []
    with torch.no_grad():
        for batch in test_loader:
            inputs = {k: v.to(DEVICE) for k, v in batch.items() if k != 'labels'}
            labels = batch['labels'].to(DEVICE)
            outputs = final_model(**inputs)
            preds = torch.argmax(outputs.logits, dim=1)
            all_preds.extend(preds.cpu().numpy())
            all_targets.extend(labels.cpu().numpy())

    print(classification_report(all_targets, all_preds))

    # --- Save Final Artifacts ---
    print("💾 Saving RoBERTa Artifacts...")
    final_model.save_pretrained(os.path.join(MODELS_DIR, 'roberta_sentiment_model'))
    tokenizer.save_pretrained(os.path.join(MODELS_DIR, 'roberta_sentiment_model'))
    
    config = {
        "model_architecture": "roberta_base",
        "max_len": MAX_LEN,
        "labels_mapping": {"0": "negative", "1": "neutral", "2": "positive"},
        "optuna_best_hyperparameters": best_params
    }
    with open(os.path.join(MODELS_DIR, 'roberta_inference_config.json'), 'w') as f:
        json.dump(config, f, indent=4)

    if os.path.exists(checkpoint_path):
        os.remove(checkpoint_path)

    print(f"✅ RoBERTa artifacts saved successfully to {MODELS_DIR}")

if __name__ == "__main__":
    train_roberta_pipeline()
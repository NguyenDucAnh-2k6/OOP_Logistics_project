# python_model/train_lstm_sentiment.py
import pandas as pd
import numpy as np
import torch
import torch.nn as nn
from torch.utils.data import DataLoader, TensorDataset
from sklearn.model_selection import train_test_split
from transformers import AutoTokenizer
from sklearn.metrics import classification_report
import os
import json
import optuna
import warnings

warnings.filterwarnings("ignore")

# 1. Global Configurations
MAX_LEN = 128
BATCH_SIZE = 64
EMBED_DIM = 128
EPOCHS_PER_TRIAL = 3 
FINAL_EPOCHS = 5
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# 2. Dynamic LSTM Architecture
class SentimentLSTM(nn.Module):
    def __init__(self, vocab_size, embed_dim, hidden_dim, output_dim, num_layers, dropout):
        super(SentimentLSTM, self).__init__()
        self.embedding = nn.Embedding(vocab_size, embed_dim, padding_idx=0)
        lstm_drop = dropout if num_layers > 1 else 0.0
        
        self.lstm = nn.LSTM(embed_dim, hidden_dim, num_layers=num_layers, 
                            batch_first=True, bidirectional=True, dropout=lstm_drop)
        self.fc = nn.Linear(hidden_dim * 2, output_dim)
        
    def forward(self, x):
        embedded = self.embedding(x)
        lstm_out, (hidden, cell) = self.lstm(embedded)
        hidden = torch.cat((hidden[-2,:,:], hidden[-1,:,:]), dim=1)
        output = self.fc(hidden)
        return output

# 3. Optuna Objective Function
def objective(trial, train_loader, val_loader, vocab_size):
    hidden_dim = trial.suggest_categorical('hidden_dim', [64, 128, 256])
    num_layers = trial.suggest_int('num_layers', 1, 3)
    dropout = trial.suggest_float('dropout', 0.1, 0.5)
    lr = trial.suggest_float('lr', 1e-4, 1e-2, log=True)
    
    model = SentimentLSTM(vocab_size, EMBED_DIM, hidden_dim, 3, num_layers, dropout).to(DEVICE)
    criterion = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=lr)
    
    for epoch in range(EPOCHS_PER_TRIAL):
        model.train()
        for inputs, labels in train_loader:
            inputs, labels = inputs.to(DEVICE), labels.to(DEVICE)
            optimizer.zero_grad()
            outputs = model(inputs)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()
            
    model.eval()
    val_loss = 0.0
    with torch.no_grad():
        for inputs, labels in val_loader:
            inputs, labels = inputs.to(DEVICE), labels.to(DEVICE)
            outputs = model(inputs)
            loss = criterion(outputs, labels)
            val_loss += loss.item()
            
    avg_val_loss = val_loss / len(val_loader)
    print(f"🟢 Trial {trial.number} | Val Loss: {avg_val_loss:.4f} | hidden:{hidden_dim}, layers:{num_layers}, lr:{lr:.5f}")
    return avg_val_loss


def train_LSTM_pipeline():
    print(f"🚀 Starting LSTM Pipeline on {DEVICE}...")
    
    SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
    DATA_DIR = os.path.join(SCRIPT_DIR, 'data')
    PROCESSED_DIR = os.path.join(DATA_DIR, 'processed')
    MODELS_DIR = os.path.join(SCRIPT_DIR, 'models')
    
    os.makedirs(PROCESSED_DIR, exist_ok=True)
    os.makedirs(MODELS_DIR, exist_ok=True)

    # We always load the tokenizer purely to get the vocab_size for the neural network
    print("Loading Tokenizer architecture...")
    tokenizer = AutoTokenizer.from_pretrained("google-bert/bert-base-multilingual-cased")
    vocab_size = tokenizer.vocab_size

    # Define paths for our cached PyTorch tensors
    train_seq_path = os.path.join(PROCESSED_DIR, 'lstm_train_seq.pt')
    train_lbl_path = os.path.join(PROCESSED_DIR, 'lstm_train_lbl.pt')
    val_seq_path = os.path.join(PROCESSED_DIR, 'lstm_val_seq.pt')
    val_lbl_path = os.path.join(PROCESSED_DIR, 'lstm_val_lbl.pt')
    test_seq_path = os.path.join(PROCESSED_DIR, 'lstm_test_seq.pt')
    test_lbl_path = os.path.join(PROCESSED_DIR, 'lstm_test_lbl.pt')

    # --- CACHING LOGIC ---
    if os.path.exists(train_seq_path) and os.path.exists(test_lbl_path):
        print("⚡ Found cached tokenized tensors! Loading them directly to save time...")
        X_train_seq = torch.load(train_seq_path)
        y_train_tensor = torch.load(train_lbl_path)
        X_val_seq = torch.load(val_seq_path)
        y_val_tensor = torch.load(val_lbl_path)
        X_test_seq = torch.load(test_seq_path)
        y_test_tensor = torch.load(test_lbl_path)

    else:
        print("No cache found. Loading Dataset & Injecting Neutral Data...")
        df = pd.read_csv(os.path.join(DATA_DIR, "IMDB-Dataset.csv"))
        df['label'] = df['sentiment'].map({'negative': 0, 'positive': 2})

        neutral_texts = ["This is an okay movie.", "I have no strong feelings.", "It was average."]
        df_neutral = pd.DataFrame({'review': neutral_texts * 1000, 'label': [1] * 3000})
        df = pd.concat([df, df_neutral], ignore_index=True)

        X_train, X_temp, y_train, y_temp = train_test_split(df['review'].values, df['label'].values, test_size=0.2, random_state=42)
        X_val, X_test, y_val, y_test = train_test_split(X_temp, y_temp, test_size=0.5, random_state=42)

        print("Tokenizing Text (Converting words to sequence tensors)...")
        def encode_texts(texts):
            encoded = tokenizer(texts.tolist(), padding='max_length', truncation=True, 
                                max_length=MAX_LEN, return_tensors='pt')
            return encoded['input_ids']

        X_train_seq = encode_texts(X_train)
        y_train_tensor = torch.tensor(y_train, dtype=torch.long)
        
        X_val_seq = encode_texts(X_val)
        y_val_tensor = torch.tensor(y_val, dtype=torch.long)
        
        X_test_seq = encode_texts(X_test)
        y_test_tensor = torch.tensor(y_test, dtype=torch.long)

        print("💾 Saving tokenized tensors to data/processed/ for future runs...")
        torch.save(X_train_seq, train_seq_path)
        torch.save(y_train_tensor, train_lbl_path)
        torch.save(X_val_seq, val_seq_path)
        torch.save(y_val_tensor, val_lbl_path)
        torch.save(X_test_seq, test_seq_path)
        torch.save(y_test_tensor, test_lbl_path)

    # --- Create DataLoaders ---
    train_data = TensorDataset(X_train_seq, y_train_tensor)
    val_data = TensorDataset(X_val_seq, y_val_tensor)
    test_data = TensorDataset(X_test_seq, y_test_tensor)

    train_loader = DataLoader(train_data, batch_size=BATCH_SIZE, shuffle=True)
    val_loader = DataLoader(val_data, batch_size=BATCH_SIZE)
    test_loader = DataLoader(test_data, batch_size=BATCH_SIZE)

    # --- Optuna Optimization ---
    print("\n🚀 Starting Optuna Optimization for LSTM...")
    study = optuna.create_study(direction='minimize')
    study.optimize(lambda trial: objective(trial, train_loader, val_loader, vocab_size), n_trials=2)

    print("\n✅ Optimization Finished!")
    best_params = study.best_params
    print(f"Best Hyperparameters: {best_params}")

    # --- Final Model Training ---
    print("\nTraining Final LSTM Model with Best Parameters...")
    final_model = SentimentLSTM(vocab_size, EMBED_DIM, best_params['hidden_dim'], 3, 
                                best_params['num_layers'], best_params['dropout']).to(DEVICE)
    
    criterion = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(final_model.parameters(), lr=best_params['lr'])

    for epoch in range(FINAL_EPOCHS):
        final_model.train()
        total_loss = 0
        for batch_idx, (inputs, labels) in enumerate(train_loader):
            inputs, labels = inputs.to(DEVICE), labels.to(DEVICE)
            optimizer.zero_grad()
            outputs = final_model(inputs)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()
            total_loss += loss.item()
            
            if batch_idx % 100 == 0:
                print(f"Epoch {epoch+1}/{FINAL_EPOCHS} | Batch {batch_idx}/{len(train_loader)} | Loss: {loss.item():.4f}")

    # --- Evaluation ---
    print("\nEvaluating Final LSTM on Test Set...")
    final_model.eval()
    all_preds, all_targets = [], []
    with torch.no_grad():
        for inputs, labels in test_loader:
            inputs = inputs.to(DEVICE)
            outputs = final_model(inputs)
            _, preds = torch.max(outputs, 1)
            all_preds.extend(preds.cpu().numpy())
            all_targets.extend(labels.numpy())

    print(classification_report(all_targets, all_preds))

    # --- Save Artifacts ---
    print("💾 Saving LSTM Artifacts...")
    torch.save(final_model.state_dict(), os.path.join(MODELS_DIR, 'lstm_sentiment_model.pt'))
    
    config = {
        "model_architecture": "lstm_bidirectional",
        "tokenizer": "google-bert/bert-base-multilingual-cased",
        "max_len": MAX_LEN,
        "embed_dim": EMBED_DIM,
        "labels_mapping": {"0": "negative", "1": "neutral", "2": "positive"},
        "optuna_best_hyperparameters": best_params,
        "validation_loss": study.best_value
    }
    with open(os.path.join(MODELS_DIR, 'lstm_inference_config.json'), 'w') as f:
        json.dump(config, f, indent=4)

    print(f"✅ LSTM artifacts saved successfully to {MODELS_DIR}")

if __name__ == "__main__":
    train_LSTM_pipeline()
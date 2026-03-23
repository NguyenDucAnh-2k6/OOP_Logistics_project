# python_model/train_xgb_sentiment.py
import pandas as pd
import numpy as np
import xgboost as xgb
from sklearn.model_selection import train_test_split
from sentence_transformers import SentenceTransformer
from sklearn.metrics import classification_report, log_loss
import os
import warnings
import optuna
import json

# Suppress harmless multiprocessing teardown warnings in Python 3.12
warnings.filterwarnings("ignore")
os.environ["TOKENIZERS_PARALLELISM"] = "false"

def objective(trial, X_train, y_train, X_val, y_val):
    """Optuna objective function to tune XGBoost hyperparameters"""
    
    param = {
        'objective': 'multi:softprob',
        'num_class': 3,
        'eval_metric': 'mlogloss',
        'n_estimators': trial.suggest_int('n_estimators', 100, 500),
        'learning_rate': trial.suggest_float('learning_rate', 0.01, 0.3, log=True),
        'max_depth': trial.suggest_int('max_depth', 3, 9),
        'subsample': trial.suggest_float('subsample', 0.6, 1.0),
        'colsample_bytree': trial.suggest_float('colsample_bytree', 0.6, 1.0),
        'min_child_weight': trial.suggest_int('min_child_weight', 1, 7),
        'gamma': trial.suggest_float('gamma', 1e-8, 1.0, log=True),
    }

    model = xgb.XGBClassifier(**param, early_stopping_rounds=10)
    
    model.fit(
        X_train, y_train,
        eval_set=[(X_val, y_val)],
        verbose=True # Keep console clean during trials
    )

    preds_proba = model.predict_proba(X_val)
    loss = log_loss(y_val, preds_proba)
    
    return loss

def train_XGBoost_pipeline():
    print("Loading VN E-commerce Dataset & Setting Paths...")
    SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
    DATA_DIR = os.path.join(SCRIPT_DIR, 'data')
    CSV_DIR = os.path.join(DATA_DIR, "VNEcommerce.csv")
    PROCESSED_DIR = os.path.join(DATA_DIR, 'processed')
    MODELS_DIR = os.path.join(SCRIPT_DIR, 'models')
    
    os.makedirs(PROCESSED_DIR, exist_ok=True)
    os.makedirs(MODELS_DIR, exist_ok=True)
    
    # 1. Load and Map the Data
    df = pd.read_csv(CSV_DIR) 
    
    # Standardize column names (just in case they have spaces)
    df.columns = df.columns.str.strip()
    
    # Map the text labels to XGBoost numeric requirements
    label_mapping = {'NEG': 0, 'NEU': 1, 'POS': 2}
    df['target'] = df['label'].str.strip().str.upper().map(label_mapping)
    
    # Drop rows with missing comments or unmapped labels
    df = df.dropna(subset=['comment', 'target'])
    df['target'] = df['target'].astype(int)
    
    # 2. Split the Data
    X_train, X_temp, y_train, y_temp = train_test_split(df['comment'], df['target'], test_size=0.2, random_state=42)
    X_val, X_test, y_val, y_test = train_test_split(X_temp, y_temp, test_size=0.5, random_state=42)

    # Note: Renamed cache files to prevent overlap with IMDB data
    train_vec_path = os.path.join(PROCESSED_DIR, 'X_train_vec_vn.npy')
    val_vec_path = os.path.join(PROCESSED_DIR, 'X_val_vec_vn.npy')
    test_vec_path = os.path.join(PROCESSED_DIR, 'X_test_vec_vn.npy')

    # 3. Handle Multilingual Embeddings
    if os.path.exists(train_vec_path) and os.path.exists(val_vec_path) and os.path.exists(test_vec_path):
        print("⚡ Found cached VN embeddings! Loading them directly...")
        X_train_vec = np.load(train_vec_path)
        X_val_vec = np.load(val_vec_path)
        X_test_vec = np.load(test_vec_path)
    else:
        print("Loading Multilingual Embedding Model for Vietnamese...")
        # Upgraded to a model that understands Vietnamese syntax natively
        embedder_name = 'paraphrase-multilingual-MiniLM-L12-v2' 
        embedder = SentenceTransformer(embedder_name)
        
        print("Computing Embeddings (This will take a few minutes)...")
        X_train_vec = embedder.encode(X_train.tolist(), show_progress_bar=True)
        X_val_vec = embedder.encode(X_val.tolist(), show_progress_bar=True)
        X_test_vec = embedder.encode(X_test.tolist(), show_progress_bar=True)
        
        print("💾 Saving VN embeddings to data/processed/ for future runs...")
        np.save(train_vec_path, X_train_vec)
        np.save(val_vec_path, X_val_vec)
        np.save(test_vec_path, X_test_vec)

    # (REMOVED IMDB DUMMY NEUTRAL INJECTION BLOCK - VN DATA ALREADY HAS NEUTRAL)

    # 4. Optuna Hyperparameter Tuning
    print("\n🚀 Starting Optuna Hyperparameter Optimization...")
    study = optuna.create_study(direction='minimize')
    study.optimize(lambda trial: objective(trial, X_train_vec, y_train, X_val_vec, y_val), n_trials=25)

    print("\n✅ Optimization Finished!")
    print("Best validation log-loss:", study.best_value)
    print("Best Hyperparameters:")
    for key, value in study.best_params.items():
        print(f"  {key}: {value}")

    # 5. Train Final Model
    print("\nTraining Final Model with Best Parameters...")
    best_params = study.best_params
    best_params['objective'] = 'multi:softprob'
    best_params['num_class'] = 3
    best_params['eval_metric'] = 'mlogloss'

    final_model = xgb.XGBClassifier(**best_params, early_stopping_rounds=10)
    final_model.fit(
        X_train_vec, y_train,
        eval_set=[(X_val_vec, y_val)],
        verbose=True
    )

    print("\nEvaluating Final Model on Test Set...")
    preds = final_model.predict(X_test_vec)
    print(classification_report(y_test, preds, target_names=['Negative (0)', 'Neutral (1)', 'Positive (2)']))

    # 6. Save Artifacts
    print("💾 Saving Model Artifacts...")
    if not hasattr(final_model, "_estimator_type"):
        final_model._estimator_type = "classifier"
        
    model_path = os.path.join(MODELS_DIR, 'xgb_sentiment_model.json')
    final_model.save_model(model_path)
    
    # Save the inference config so your FastAPI server knows how to load it
    inference_config = {
        "model_architecture": "xgboost",
        "embedder_model": "paraphrase-multilingual-MiniLM-L12-v2", # Must match embedder used above
        "labels_mapping": {
            "0": "negative", 
            "1": "neutral", 
            "2": "positive"
        },
        "optuna_best_hyperparameters": study.best_params,
        "validation_log_loss": study.best_value
    }
    
    config_path = os.path.join(MODELS_DIR, 'xgb_inference_config.json')
    with open(config_path, 'w', encoding='utf-8') as f:
        json.dump(inference_config, f, indent=4)
        
    print(f"Artifacts saved successfully to {MODELS_DIR}")

if __name__ == "__main__":
    train_XGBoost_pipeline()
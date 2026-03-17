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
    
    # Define the hyperparameter search space
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
        'early_stopping_rounds': 10
    }

    model = xgb.XGBClassifier(**param)
    
    model.fit(
        X_train, y_train,
        eval_set=[(X_val, y_val)],
        verbose=True # Keep console clean during trials
    )

    # Evaluate on validation set to guide Optuna
    preds_proba = model.predict_proba(X_val)
    loss = log_loss(y_val, preds_proba)
    
    return loss

def train_XGBoost_pipeline():
    print("Loading IMDB Dataset & Setting Paths...")
    SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
    DATA_DIR = os.path.join(SCRIPT_DIR, 'data')
    CSV_DIR = os.path.join(DATA_DIR, "IMDB-Dataset.csv")
    PROCESSED_DIR = os.path.join(DATA_DIR, 'processed')
    MODELS_DIR = os.path.join(SCRIPT_DIR, 'models')
    
    os.makedirs(PROCESSED_DIR, exist_ok=True)
    os.makedirs(MODELS_DIR, exist_ok=True)
    
    df = pd.read_csv(CSV_DIR) 
    df['label'] = df['sentiment'].map({'negative': 0, 'positive': 2})
    
    X_train, X_temp, y_train, y_temp = train_test_split(df['review'], df['label'], test_size=0.2, random_state=42)
    X_val, X_test, y_val, y_test = train_test_split(X_temp, y_temp, test_size=0.5, random_state=42)

    train_vec_path = os.path.join(PROCESSED_DIR, 'X_train_vec.npy')
    val_vec_path = os.path.join(PROCESSED_DIR, 'X_val_vec.npy')
    test_vec_path = os.path.join(PROCESSED_DIR, 'X_test_vec.npy')

    # --- Caching Logic ---
    if os.path.exists(train_vec_path) and os.path.exists(val_vec_path) and os.path.exists(test_vec_path):
        print("⚡ Found cached embeddings! Loading them directly...")
        X_train_vec = np.load(train_vec_path)
        X_val_vec = np.load(val_vec_path)
        X_test_vec = np.load(test_vec_path)
    else:
        print("Loading Embedding Model...")
        embedder = SentenceTransformer('all-MiniLM-L6-v2')
        print("Computing Embeddings (This will take a few minutes)...")
        X_train_vec = embedder.encode(X_train.tolist(), show_progress_bar=True)
        X_val_vec = embedder.encode(X_val.tolist(), show_progress_bar=True)
        X_test_vec = embedder.encode(X_test.tolist(), show_progress_bar=True)
        
        print("💾 Saving embeddings to data/processed/ for future runs...")
        np.save(train_vec_path, X_train_vec)
        np.save(val_vec_path, X_val_vec)
        np.save(test_vec_path, X_test_vec)

    # --- INJECT NEUTRAL DATA ---
    print("Injecting Neutral (Class 1) data to satisfy XGBoost multi-class constraints...")
    dummy_texts = [
        "This is an okay movie.", "I have no strong feelings.", 
        "It was average.", "Just a standard film.", "Neutral statement.",
        "The movie was exactly 90 minutes long.", "I watched it on a Tuesday."
    ]
    embedder_temp = SentenceTransformer('all-MiniLM-L6-v2')
    dummy_vecs = embedder_temp.encode(dummy_texts)
    dummy_labels = [1] * len(dummy_texts)
    
    X_train_vec = np.vstack((X_train_vec, dummy_vecs))
    y_train = pd.concat([y_train, pd.Series(dummy_labels)], ignore_index=True)
    
    X_val_vec = np.vstack((X_val_vec, dummy_vecs))
    y_val = pd.concat([y_val, pd.Series(dummy_labels)], ignore_index=True)

    X_test_vec = np.vstack((X_test_vec, dummy_vecs))
    y_test = pd.concat([y_test, pd.Series(dummy_labels)], ignore_index=True)

    # --- OPTUNA HYPERPARAMETER TUNING ---
    print("\n🚀 Starting Optuna Hyperparameter Optimization...")
    # Adjust n_trials based on how much time you have (20-50 is a good start)
    study = optuna.create_study(direction='minimize')
    study.optimize(lambda trial: objective(trial, X_train_vec, y_train, X_val_vec, y_val), n_trials=2)

    print("\n✅ Optimization Finished!")
    print("Best validation log-loss:", study.best_value)
    print("Best Hyperparameters:")
    for key, value in study.best_params.items():
        print(f"  {key}: {value}")

    # --- TRAIN FINAL MODEL ---
    print("\nTraining Final Model with Best Parameters...")
    best_params = study.best_params
    best_params['objective'] = 'multi:softprob'
    best_params['num_class'] = 3
    best_params['eval_metric'] = 'mlogloss'
    best_params['early_stopping_rounds'] = 10

    final_model = xgb.XGBClassifier(**best_params)
    final_model.fit(
        X_train_vec, y_train,
        eval_set=[(X_val_vec, y_val)],
        verbose=True
    )

    print("\nEvaluating Final Model on Test Set...")
    preds = final_model.predict(X_test_vec)
    print(classification_report(y_test, preds))

    # --- SAVE ARTIFACTS ---
    print("💾 Saving Model Artifacts...")
    if not hasattr(final_model, "_estimator_type"):
        final_model._estimator_type = "classifier"
    # 1. Save the model weights
    model_path = os.path.join(MODELS_DIR, 'xgb_sentiment_model.json')
    final_model.save_model(model_path)
    
    # 2. Save the inference config and best hyperparameters
    inference_config = {
        "model_architecture": "xgboost",
        "embedder_model": "all-MiniLM-L6-v2",
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
# python_model/train_svm_sentiment.py
import pandas as pd
import numpy as np
from sklearn.svm import LinearSVC
from sklearn.calibration import CalibratedClassifierCV
from sklearn.model_selection import train_test_split
from sentence_transformers import SentenceTransformer
from sklearn.metrics import classification_report, accuracy_score
import os
import warnings
import json
import joblib
import optuna

warnings.filterwarnings("ignore")
os.environ["TOKENIZERS_PARALLELISM"] = "false"

def objective(trial, X_train, y_train, X_val, y_val):
    """Optuna objective function to tune LinearSVC hyperparameters"""
    
    # Define the hyperparameter search space
    param = {
        'C': trial.suggest_float('C', 1e-4, 10.0, log=True),
        'tol': trial.suggest_float('tol', 1e-5, 1e-1, log=True),
        'class_weight': trial.suggest_categorical('class_weight', [None, 'balanced']),
        'max_iter': trial.suggest_int('max_iter', 1000, 5000),
        'dual': False, # Always False when n_samples > n_features
        'random_state': 42
    }

    model = LinearSVC(**param)
    model.fit(X_train, y_train)

    # Evaluate on validation set
    preds = model.predict(X_val)
    accuracy = accuracy_score(y_val, preds)
    
    return accuracy # We want Optuna to maximize this

def train_SVM_pipeline():
    print("Loading Paths & Data...")
    SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
    DATA_DIR = os.path.join(SCRIPT_DIR, 'data')
    PROCESSED_DIR = os.path.join(DATA_DIR, 'processed')
    MODELS_DIR = os.path.join(SCRIPT_DIR, 'models')
    
    os.makedirs(MODELS_DIR, exist_ok=True)
    
    # 1. Load cached embeddings directly to skip Transformer bottleneck
    train_vec_path = os.path.join(PROCESSED_DIR, 'X_train_vec.npy')
    val_vec_path = os.path.join(PROCESSED_DIR, 'X_val_vec.npy')
    test_vec_path = os.path.join(PROCESSED_DIR, 'X_test_vec.npy')

    if not (os.path.exists(train_vec_path) and os.path.exists(val_vec_path) and os.path.exists(test_vec_path)):
        print("Error: Cached embeddings not found! Run your XGBoost training script once to generate the .npy files.")
        return

    print("⚡ Loading cached embeddings...")
    X_train_vec = np.load(train_vec_path)
    X_val_vec = np.load(val_vec_path)
    X_test_vec = np.load(test_vec_path)

    # 2. Re-create the labels for the cached splits
    CSV_DIR = os.path.join(DATA_DIR, "IMDB-Dataset.csv")
    df = pd.read_csv(CSV_DIR) 
    df['label'] = df['sentiment'].map({'negative': 0, 'positive': 2})
    
    _, X_temp, y_train, y_temp = train_test_split(df['review'], df['label'], test_size=0.2, random_state=42)
    _, _, y_val, y_test = train_test_split(X_temp, y_temp, test_size=0.5, random_state=42)

    # 3. Inject Neutral Data
    print("Injecting Neutral (Class 1) data...")
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

    # 4. Optuna Optimization
    print("\n🚀 Starting Optuna Hyperparameter Optimization...")
    # Change n_trials here later when you want to run a deeper search
    study = optuna.create_study(direction='maximize')
    study.optimize(lambda trial: objective(trial, X_train_vec, y_train, X_val_vec, y_val), n_trials=25)

    print("\n✅ Optimization Finished!")
    print("Best validation accuracy:", study.best_value)
    print("Best Hyperparameters:")
    for key, value in study.best_params.items():
        print(f"  {key}: {value}")

    # 5. Train Final Model with Best Parameters
    print("\nTraining Final Model with Best Parameters...")
    best_params = study.best_params
    best_params['dual'] = False
    best_params['random_state'] = 42

    base_svm = LinearSVC(**best_params)
    final_model = CalibratedClassifierCV(base_svm, cv=5) 
    final_model.fit(X_train_vec, y_train)

    print("\nEvaluating Final Model on Test Set...")
    preds = final_model.predict(X_test_vec)
    print(classification_report(y_test, preds))

    # 6. Save Artifacts
    print("💾 Saving Model Artifacts...")
    
    model_path = os.path.join(MODELS_DIR, 'svm_sentiment_proba_model.joblib')
    joblib.dump(final_model, model_path)
    
    inference_config = {
        "model_architecture": "linear_svc",
        "embedder_model": "all-MiniLM-L6-v2",
        "labels_mapping": {
            "0": "negative", 
            "1": "neutral", 
            "2": "positive"
        },
        "optuna_best_hyperparameters": study.best_params,
        "validation_accuracy": study.best_value
    }
    
    config_path = os.path.join(MODELS_DIR, 'svm_inference_proba_config.json')
    with open(config_path, 'w', encoding='utf-8') as f:
        json.dump(inference_config, f, indent=4)
        
    print(f"✅ SVM artifacts saved successfully to {MODELS_DIR}")

if __name__ == "__main__":
    train_SVM_pipeline()
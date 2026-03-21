# python_model/train_mlp_sentiment.py
import pandas as pd
import numpy as np
from sklearn.neural_network import MLPClassifier
from sklearn.model_selection import train_test_split
from sentence_transformers import SentenceTransformer
from sklearn.metrics import classification_report, log_loss
import os
import warnings
import json
import joblib
import optuna

warnings.filterwarnings("ignore")
os.environ["TOKENIZERS_PARALLELISM"] = "false"

def objective(trial, X_train, y_train, X_val, y_val):
    """Optuna objective function to tune MLP hyperparameters"""
    
    # We define 3 potential network architectures (shallow, medium, deep)
    n_layers = trial.suggest_int('n_layers', 1, 3)
    layers = []
    for i in range(n_layers):
        layers.append(trial.suggest_int(f'n_units_l{i}', 32, 256, log=True))
    
    param = {
        'hidden_layer_sizes': tuple(layers),
        'activation': trial.suggest_categorical('activation', ['relu', 'tanh']),
        'solver': 'adam',
        'alpha': trial.suggest_float('alpha', 1e-5, 1e-1, log=True), # L2 Regularization penalty
        'learning_rate_init': trial.suggest_float('learning_rate_init', 1e-4, 1e-2, log=True),
        'max_iter': 500,
        'early_stopping': True, # Crucial to prevent deep learning overfitting
        'validation_fraction': 0.1,
        'random_state': 42,
        'verbose': True
    }

    model = MLPClassifier(**param)
    model.fit(X_train, y_train)

    preds_proba = model.predict_proba(X_val)
    loss = log_loss(y_val, preds_proba)
    
    print(f"🟢 Trial {trial.number} finished | Val Loss: {loss:.4f} | Architecture: {param['hidden_layer_sizes']}")
    
    return loss # Minimize log loss

def train_MLP_pipeline():
    print("Loading Paths & Cached Data...")
    SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
    DATA_DIR = os.path.join(SCRIPT_DIR, 'data')
    PROCESSED_DIR = os.path.join(DATA_DIR, 'processed')
    MODELS_DIR = os.path.join(SCRIPT_DIR, 'models')
    
    os.makedirs(MODELS_DIR, exist_ok=True)
    
    train_vec_path = os.path.join(PROCESSED_DIR, 'X_train_vec.npy')
    val_vec_path = os.path.join(PROCESSED_DIR, 'X_val_vec.npy')
    test_vec_path = os.path.join(PROCESSED_DIR, 'X_test_vec.npy')

    if not (os.path.exists(train_vec_path) and os.path.exists(val_vec_path) and os.path.exists(test_vec_path)):
        print("Error: Cached embeddings not found!")
        return

    print("⚡ Loading cached embeddings...")
    X_train_vec = np.load(train_vec_path)
    X_val_vec = np.load(val_vec_path)
    X_test_vec = np.load(test_vec_path)

    CSV_DIR = os.path.join(DATA_DIR, "IMDB-Dataset.csv")
    df = pd.read_csv(CSV_DIR) 
    df['label'] = df['sentiment'].map({'negative': 0, 'positive': 2})
    
    _, X_temp, y_train, y_temp = train_test_split(df['review'], df['label'], test_size=0.2, random_state=42)
    _, _, y_val, y_test = train_test_split(X_temp, y_temp, test_size=0.5, random_state=42)

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

    print("\n🚀 Starting Optuna Optimization for Deep Neural Network...")
    study = optuna.create_study(direction='minimize')
    study.optimize(lambda trial: objective(trial, X_train_vec, y_train, X_val_vec, y_val), n_trials=25)

    print("\n✅ Optimization Finished!")
    
    print("\nTraining Final Deep Learning Model...")
    best_params = study.best_params
    
    # Reconstruct the optimal hidden layer sizes from Optuna
    n_layers = best_params.pop('n_layers')
    layers = tuple([best_params.pop(f'n_units_l{i}') for i in range(n_layers)])
    
    final_params = {
        'hidden_layer_sizes': layers,
        'solver': 'adam',
        'max_iter': 500,
        'early_stopping': True,
        'random_state': 42,
        **best_params
    }

    final_model = MLPClassifier(**final_params)
    final_model.fit(X_train_vec, y_train)

    print("\nEvaluating Final Model on Test Set...")
    preds = final_model.predict(X_test_vec)
    print(classification_report(y_test, preds))

    print("💾 Saving Model Artifacts...")
    model_path = os.path.join(MODELS_DIR, 'mlp_sentiment_model.joblib')
    joblib.dump(final_model, model_path)
    
    inference_config = {
        "model_architecture": "mlp_deep_learning",
        "embedder_model": "all-MiniLM-L6-v2",
        "labels_mapping": {"0": "negative", "1": "neutral", "2": "positive"},
        "optuna_best_hyperparameters": final_params,
        "validation_loss": study.best_value
    }
    
    with open(os.path.join(MODELS_DIR, 'mlp_inference_config.json'), 'w', encoding='utf-8') as f:
        json.dump(inference_config, f, indent=4)
        
    print(f"✅ Deep Learning MLP artifacts saved successfully to {MODELS_DIR}")

if __name__ == "__main__":
    train_MLP_pipeline()
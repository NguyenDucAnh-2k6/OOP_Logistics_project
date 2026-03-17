from typing import List, Dict
import json
import logging
import os
from transformers import pipeline
from config import SENTIMENT_CONFIG
import xgboost as xgb
from sentence_transformers import SentenceTransformer
import numpy as np
import joblib
import torch
import torch.nn as nn
from transformers import AutoTokenizer
logger = logging.getLogger(__name__)

# --- SETUP ---
# 1. Load Keywords
with open(SENTIMENT_CONFIG, "r", encoding="utf-8") as f:
    SENTIMENT_KEYWORDS = json.load(f)
    logger.debug(f"Loaded sentiment keywords: {list(SENTIMENT_KEYWORDS.keys())}")

# 2. Load HuggingFace AI (PhoBERT)
logger.info("Loading HuggingFace Sentiment AI Model...")
try:
    sentiment_pipeline = pipeline(
        "sentiment-analysis", 
        model="wonrax/phobert-base-vietnamese-sentiment",
        tokenizer="wonrax/phobert-base-vietnamese-sentiment"
    )
    logger.info("PhoBERT AI Model loaded successfully")
except Exception as e:
    logger.warning(f"Failed to load PhoBERT AI Model: {e}. Will fallback to keyword.")
    sentiment_pipeline = None

# 3. Load XGBoost AI
logger.info("Loading XGBoost Sentiment Model...")
xgb_model = None
xgb_embedder = None
try:
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    model_path = os.path.join(base_dir, 'models', 'xgb_sentiment_model.json')
    
    if os.path.exists(model_path):
        xgb_embedder = SentenceTransformer('all-MiniLM-L6-v2')
        xgb_model = xgb.XGBClassifier()
        xgb_model._estimator_type = "classifier"
        xgb_model.load_model(model_path)
        xgb_model.n_classes_ = 3
        xgb_model.classes_ = np.array([0, 1, 2])
        logger.info("XGBoost Model and Embedder loaded successfully.")
    else:
        logger.warning(f"XGBoost model not found at {model_path}. Please run train_xgb_sentiment.py first.")
except Exception as e:
    logger.error(f"Failed to load XGBoost pipeline: {e}")
logger.info("Loading SVM Sentiment Model...")
svm_model = None
try:
    model_path_svm = os.path.join(base_dir, 'models', 'svm_sentiment_model.joblib')
    if os.path.exists(model_path_svm):
        svm_model = joblib.load(model_path_svm)
        logger.info("SVM Model loaded successfully.")
    else:
        logger.warning(f"SVM model not found at {model_path_svm}.")
except Exception as e:
    logger.error(f"Failed to load SVM pipeline: {e}")
logger.info("Loading MLP Sentiment Model...")
mlp_model = None
try:
    model_path_mlp = os.path.join(base_dir, 'models', 'mlp_sentiment_model.joblib')
    if os.path.exists(model_path_mlp):
        mlp_model = joblib.load(model_path_mlp)
        logger.info("MLP Deep Learning Model loaded successfully.")
    else:
        logger.warning(f"MLP model not found at {model_path_mlp}.")
except Exception as e:
    logger.error(f"Failed to load MLP pipeline: {e}")
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

logger.info("Loading LSTM Sentiment Model...")
lstm_model = None
lstm_tokenizer = None
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")
LSTM_MAX_LEN = 128

try:
    model_path_lstm = os.path.join(base_dir, 'models', 'lstm_sentiment_model.pt')
    config_path_lstm = os.path.join(base_dir, 'models', 'lstm_inference_config.json')
    
    if os.path.exists(model_path_lstm) and os.path.exists(config_path_lstm):
        with open(config_path_lstm, 'r') as f:
            lstm_config = json.load(f)
            
        lstm_tokenizer = AutoTokenizer.from_pretrained(lstm_config["tokenizer"])
        LSTM_MAX_LEN = lstm_config["max_len"]
        best_params = lstm_config["optuna_best_hyperparameters"]
        
        # Reconstruct Architecture
        lstm_model = SentimentLSTM(
            vocab_size=lstm_tokenizer.vocab_size,
            embed_dim=lstm_config["embed_dim"],
            hidden_dim=best_params["hidden_dim"],
            output_dim=3,
            num_layers=best_params["num_layers"],
            dropout=best_params["dropout"]
        ).to(DEVICE)
        
        # Load Weights & Set to Eval Mode (Crucial for Dropout!)
        lstm_model.load_state_dict(torch.load(model_path_lstm, map_location=DEVICE))
        lstm_model.eval() 
        logger.info("LSTM Deep Learning Model loaded successfully.")
    else:
        logger.warning("LSTM artifacts not found. Please run train_lstm_sentiment.py.")
except Exception as e:
    logger.error(f"Failed to load LSTM pipeline: {e}")
# --- LOGIC ---

def predict_keyword(text: str) -> str:
    text_lower = text.lower()
    score = 0
    for word in SENTIMENT_KEYWORDS.get("positive", []):
        if word in text_lower: score += 1
    for word in SENTIMENT_KEYWORDS.get("negative", []):
        if word in text_lower: score -= 1
        
    if score > 0: return "positive"
    elif score < 0: return "negative"
    return "neutral"

def predict_ai_batch(texts: List[str]) -> List[str]:
    results = []
    if sentiment_pipeline is None:
        return [predict_keyword(t) for t in texts] # Fallback

    batch_size = 16
    for i in range(0, len(texts), batch_size):
        batch = [t[:256] for t in texts[i:i+batch_size]] # Truncate to 256 chars
        try:
            preds = sentiment_pipeline(batch)
            for p in preds:
                label = p['label']
                if label == "POS": results.append("positive")
                elif label == "NEG": results.append("negative")
                else: results.append("neutral")
        except:
            results.extend(["neutral"] * len(batch))
    return results
def predict_svm_batch(texts: List[str]) -> List[str]:
    if not texts:
        return []
        
    # We can reuse the xgb_embedder since it is the exact same SentenceTransformer model!
    if svm_model is None or xgb_embedder is None:
        logger.warning("SVM not loaded. Falling back to PhoBERT AI.")
        return predict_ai_batch(texts)

    embeddings = xgb_embedder.encode(texts)
    predictions = svm_model.predict(embeddings)
    
    mapping = {0: 'negative', 1: 'neutral', 2: 'positive'}
    return [mapping.get(int(pred), 'neutral') for pred in predictions]
def predict_xgboost_batch(texts: List[str]) -> List[str]:
    if not texts:
        return []
        
    if xgb_model is None or xgb_embedder is None:
        logger.warning("XGBoost not loaded. Falling back to PhoBERT AI.")
        return predict_ai_batch(texts)

    # 1. Compute embeddings for incoming batch
    embeddings = xgb_embedder.encode(texts)
    
    # 2. Predict using the loaded XGBoost model
    predictions = xgb_model.predict(embeddings)
    
    # 3. Map predictions back to string categories
    mapping = {0: 'negative', 1: 'neutral', 2: 'positive'}
    return [mapping.get(int(pred), 'neutral') for pred in predictions]
def predict_mlp_batch(texts: List[str]) -> List[str]:
    if not texts:
        return []
        
    # Reuse the same SentenceTransformer embedder
    if mlp_model is None or xgb_embedder is None:
        logger.warning("MLP not loaded. Falling back to PhoBERT AI.")
        return predict_ai_batch(texts)

    embeddings = xgb_embedder.encode(texts)
    predictions = mlp_model.predict(embeddings)
    
    mapping = {0: 'negative', 1: 'neutral', 2: 'positive'}
    return [mapping.get(int(pred), 'neutral') for pred in predictions]
def predict_lstm_batch(texts: List[str]) -> List[str]:
    if not texts: return []
    if lstm_model is None or lstm_tokenizer is None:
        logger.warning("LSTM not loaded. Falling back to PhoBERT AI.")
        return predict_ai_batch(texts)
        
    # 1. Tokenize sequence
    encoded = lstm_tokenizer(texts, padding='max_length', truncation=True, 
                             max_length=LSTM_MAX_LEN, return_tensors='pt')
    inputs = encoded['input_ids'].to(DEVICE)
    
    # 2. Infer without tracking gradients
    with torch.no_grad():
        outputs = lstm_model(inputs)
        _, preds = torch.max(outputs, 1)
        
    # 3. Map predictions
    mapping = {0: 'negative', 1: 'neutral', 2: 'positive'}
    return [mapping.get(int(p), 'neutral') for p in preds.cpu().numpy()]
def aggregate_by_date(texts: List[str], dates: List[str], model_type: str = "ai") -> List[Dict]:
    stats = {}
    
    print(f"📊 Analyzing Sentiment using [{model_type.upper()}] model...")
    
    # Route logic based on UI input (keyword, ai, xgboost)
    if model_type == "keyword":
        predictions = [predict_keyword(t) for t in texts]
    elif model_type == "xgboost":
        predictions = predict_xgboost_batch(texts)
    elif model_type == "svm":
        predictions = predict_svm_batch(texts) # Route SVM requests
    elif model_type == "mlp":
        predictions = predict_mlp_batch(texts) # <-- Add MLP route
    elif model_type == "lstm":
        predictions = predict_lstm_batch(texts) # <-- Add LSTM route
    else:
        predictions = predict_ai_batch(texts) # Default to PhoBERT

    for date, senti in zip(dates, predictions):
        if date not in stats:
            stats[date] = {"positive": 0, "neutral": 0, "negative": 0}
        stats[date][senti] += 1
        
    # Convert dictionary stats into the final list format required by Java backend
    return [{"date": k, **v} for k, v in sorted(stats.items())]
from typing import List, Dict
import json
import logging
import os
from transformers import pipeline
from config import SENTIMENT_CONFIG
import xgboost as xgb
from sentence_transformers import SentenceTransformer
import numpy as np
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

def aggregate_by_date(texts: List[str], dates: List[str], model_type: str = "ai") -> List[Dict]:
    stats = {}
    
    print(f"📊 Analyzing Sentiment using [{model_type.upper()}] model...")
    
    # Route logic based on UI input (keyword, ai, xgboost)
    if model_type == "keyword":
        predictions = [predict_keyword(t) for t in texts]
    elif model_type == "xgboost":
        predictions = predict_xgboost_batch(texts)
    else:
        predictions = predict_ai_batch(texts) # Default to PhoBERT

    for date, senti in zip(dates, predictions):
        if date not in stats:
            stats[date] = {"positive": 0, "neutral": 0, "negative": 0}
        stats[date][senti] += 1
        
    # Convert dictionary stats into the final list format required by Java backend
    return [{"date": k, **v} for k, v in sorted(stats.items())]
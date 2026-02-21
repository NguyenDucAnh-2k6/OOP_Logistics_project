from typing import List, Dict
import json
import logging
from transformers import pipeline
from config import SENTIMENT_CONFIG

logger = logging.getLogger(__name__)

# --- SETUP ---
# Load Keywords
with open(SENTIMENT_CONFIG, "r", encoding="utf-8") as f:
    SENTIMENT_KEYWORDS = json.load(f)
    logger.debug(f"Loaded sentiment keywords: {list(SENTIMENT_KEYWORDS.keys())}")
    # Expected JSON: {"positive": ["tốt", ...], "negative": ["buồn", ...]}

# Load AI
logger.info("Loading Sentiment AI Model...")
try:
    sentiment_pipeline = pipeline(
        "sentiment-analysis", 
        model="wonrax/phobert-base-vietnamese-sentiment",
        tokenizer="wonrax/phobert-base-vietnamese-sentiment"
    )
    logger.info("Sentiment AI Model loaded successfully")
except Exception as e:
    logger.warning(f"Failed to load Sentiment AI Model: {e}. Will use keyword-based prediction")
    sentiment_pipeline = None

# --- LOGIC ---

def predict_keyword(text: str) -> str:
    text_lower = text.lower()
    score = 0
    
    # Simple scoring: +1 for positive word, -1 for negative word
    for word in SENTIMENT_KEYWORDS.get("positive", []):
        if word in text_lower: score += 1
    for word in SENTIMENT_KEYWORDS.get("negative", []):
        if word in text_lower: score -= 1
        
    if score > 0: return "positive"
    if score < 0: return "negative"
    return "neutral"

def predict_ai_batch(texts: List[str]) -> List[str]:
    results = []
    if sentiment_pipeline is None:
        return [predict_keyword(t) for t in texts] # Fallback

    # Batch process for speed
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

def aggregate_by_date(texts: List[str], dates: List[str], model_type: str = "ai") -> List[Dict]:
    stats = {}
    
    print(f"📊 Analyzing Sentiment using [{model_type.upper()}] model...")
    
    if model_type == "keyword":
        predictions = [predict_keyword(t) for t in texts]
    else:
        predictions = predict_ai_batch(texts)

    for date, senti in zip(dates, predictions):
        if date not in stats:
            stats[date] = {"positive": 0, "negative": 0, "neutral": 0}
        stats[date][senti] += 1

    # Convert to list for Java
    return [
        {"date": d, "positive": v["positive"], "negative": v["negative"], "neutral": v["neutral"]}
        for d, v in stats.items()
    ]
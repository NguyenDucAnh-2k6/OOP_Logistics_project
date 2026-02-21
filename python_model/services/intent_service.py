from typing import List, Dict
import logging
from transformers import pipeline
import json
from config import INTENT_CONFIG

logger = logging.getLogger(__name__)

# 1. LOAD EXTERNAL KEYWORDS DYNAMICALLY
try:
    with open(INTENT_CONFIG, "r", encoding="utf-8") as f:
        INTENT_KEYWORDS = json.load(f)
        INTENT_LABELS = list(INTENT_KEYWORDS.keys())
        logger.debug(f"Loaded intent keywords: {INTENT_LABELS}")
except Exception as e:
    logger.error(f"Error loading intent config: {e}")
    INTENT_KEYWORDS = {}
    INTENT_LABELS = ["Yêu cầu cứu trợ (Request/Demand)", "Cung cấp cứu trợ (Offer/Supply)", "Tin tức chung (General News/Info)"]

# Attempt to share the model already loaded in relief_service to save RAM
try:
    from services.relief_service import classifier
except ImportError:
    classifier = None

# Fallback if it wasn't loaded
if classifier is None:
    logger.info("Loading Intent AI Model...")
    try:
        classifier = pipeline(
            "zero-shot-classification", 
            model="MoritzLaurer/mDeBERTa-v3-base-mnli-xnli",
            device=-1 
        )
        logger.info("Intent AI Model loaded successfully")
    except Exception as e:
        logger.error(f"Failed to load Intent AI Model: {e}")

# Define the logical categories
INTENT_LABELS = [
    "Yêu cầu cứu trợ (Request/Demand)", 
    "Cung cấp cứu trợ (Offer/Supply)", 
    "Tin tức chung (General News/Info)"
]

def classify_intent_batch(texts: List[str], model_type: str = "ai") -> List[str]:
    """
    Classifies whether a text is asking for help, offering help, or just news.
    """
    if model_type == "keyword" or classifier is None or not texts:
        return _fallback_keyword_intent(texts)

    results = []
    batch_size = 8
    
    logger.debug(f"AI Classifying Intent for {len(texts)} items...")

    for i in range(0, len(texts), batch_size):
        batch = texts[i:i+batch_size]
        try:
            # multi_label=False because a post is usually predominantly one intent
            preds = classifier(batch, INTENT_LABELS, multi_label=False, truncation=True)
            for p in preds:
                # The first label in the list is the one with the highest confidence score
                results.append(p['labels'][0])
        except Exception as e:
            print(f"❌ Intent Batch Error: {e}")
            results.extend(["Tin tức chung (General News/Info)"] * len(batch))
            
    return results

def _fallback_keyword_intent(texts: List[str]) -> List[str]:
    results = []
    for text in texts:
        t = text.lower()
        found = False
        
        # Dynamically loop through categories and their keywords
        for label, kws in INTENT_KEYWORDS.items():
            if any(kw in t for kw in kws if kw.strip()):
                results.append(label)
                found = True
                break
                
        if not found:
            results.append("Tin tức chung (General News/Info)")
            
    return results

def aggregate_intent_stats(texts: List[str], model_type: str = "ai") -> Dict[str, int]:
    """Aggregates the counts for the API response"""
    predictions = classify_intent_batch(texts, model_type)
    
    stats = {
        "Request": 0,
        "Offer": 0,
        "News": 0
    }
    
    for p in predictions:
        if "Request" in p: stats["Request"] += 1
        elif "Offer" in p: stats["Offer"] += 1
        else: stats["News"] += 1
        
    return stats
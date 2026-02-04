import json
from typing import List, Dict
from transformers import pipeline
from config import RELIEF_CONFIG

# Import sentiment functions from the sibling service
# Ensure sentiment_service has 'predict_ai_batch' and 'predict_keyword' exposed
try:
    from .sentiment_service import predict_ai_batch, predict_keyword
except ImportError:
    from services.sentiment_service import predict_ai_batch, predict_keyword

# --- 1. SETUP: Load Keywords ---
try:
    with open(RELIEF_CONFIG, "r", encoding="utf-8") as f:
        RELIEF_KEYWORDS = json.load(f)
        CANDIDATE_LABELS = list(RELIEF_KEYWORDS.keys())
        # Remove 'Other' from candidates for AI (it will be the fallback)
        if "Other" in CANDIDATE_LABELS:
            CANDIDATE_LABELS.remove("Other")
except Exception as e:
    print(f"❌ Error loading relief config: {e}")
    RELIEF_KEYWORDS = {}
    CANDIDATE_LABELS = []

# --- 2. SETUP: Load AI Model (Lazy Loading) ---
print("⏳ Loading Relief Classification AI Model...")
try:
    # We use the same model as Damage Service to save memory if possible
    # 'mnli-xnli' is excellent for Zero-Shot classification
    classifier = pipeline(
        "zero-shot-classification", 
        model="MoritzLaurer/mDeBERTa-v3-base-mnli-xnli",
        device=-1 
    )
    print("✅ Relief AI Model Loaded.")
except Exception as e:
    print(f"❌ Relief AI Load Failed: {e}")
    classifier = None

# --- CORE FUNCTIONS ---

def detect_relief_keyword(text: str) -> List[str]:
    """
    Original keyword logic: Returns list of categories found.
    """
    if not text:
        return ["Other"]

    text_lower = text.lower()
    cats = []
    for cat, kws in RELIEF_KEYWORDS.items():
        if any(kw in text_lower for kw in kws):
            cats.append(cat)

    return cats if cats else ["Other"]

def detect_relief_ai_batch(texts: List[str]) -> List[List[str]]:
    """
    AI Logic: Uses Zero-Shot Classification to find categories.
    Supports MULTI-LABEL (e.g., a text can be both 'Food' and 'Medical').
    """
    if classifier is None or not texts:
        return [detect_relief_keyword(t) for t in texts] # Fallback

    results = []
    batch_size = 8 # Process 8 items at a time
    
    print(f"🧠 AI Classifying Relief Needs for {len(texts)} items...")

    for i in range(0, len(texts), batch_size):
        batch_texts = texts[i:i+batch_size]
        try:
            # multi_label=True is KEY here. It allows independent probabilities for each label.
            predictions = classifier(
                batch_texts, 
                CANDIDATE_LABELS, 
                multi_label=True, 
                truncation=True
            )
            
            for pred in predictions:
                # pred['labels'] are sorted by score. pred['scores'] matches that order.
                selected_labels = []
                
                # We iterate and pick any label with > 40% confidence
                for label, score in zip(pred['labels'], pred['scores']):
                    if score > 0.4:
                        selected_labels.append(label)
                
                # If nothing matched with high confidence, mark as Other
                if not selected_labels:
                    results.append(["Other"])
                else:
                    results.append(selected_labels)
                    
        except Exception as e:
            print(f"❌ AI Batch Error: {e}")
            # Graceful fallback to keyword for this batch
            results.extend([detect_relief_keyword(t) for t in batch_texts])
            
    return results

def get_sentiments_batch(texts: List[str], model_type: str) -> List[str]:
    """Helper to route sentiment request to the correct engine"""
    if model_type == "keyword":
        # Process one by one for keyword
        return [predict_keyword(t) for t in texts]
    else:
        # Process in batch for AI
        return predict_ai_batch(texts)

# --- MAIN EXPORTED FUNCTIONS ---

def aggregate_relief_sentiment(texts: List[str], model_type: str = "ai") -> Dict[str, Dict[str, int]]:
    """
    Problem 3: Aggregate sentiment by relief category.
    """
    stats = {} # { "Food": {pos: 1, neg: 0}, ... }

    # 1. Get Categories (Batch)
    if model_type == "keyword":
        print("⚡ Using Keyword Mode for Relief Categories")
        all_categories = [detect_relief_keyword(t) for t in texts]
    else:
        print("🧠 Using AI Mode for Relief Categories")
        all_categories = detect_relief_ai_batch(texts)

    # 2. Get Sentiments (Batch)
    all_sentiments = get_sentiments_batch(texts, model_type)

    # 3. Aggregate
    # Note: One text can contribute to multiple categories (e.g. Food & Water)
    for cats, senti in zip(all_categories, all_sentiments):
        for cat in cats:
            if cat not in stats:
                stats[cat] = {"positive": 0, "negative": 0, "neutral": 0}
            stats[cat][senti] += 1

    return stats


def aggregate_relief_time_series(texts: List[str], dates: List[str], model_type: str = "ai"):
    """
    Problem 4: Gom theo ngày + loại hàng cứu trợ.
    """
    if len(texts) != len(dates):
        raise ValueError("texts và dates phải có cùng số lượng phần tử")

    # 1. Get Categories (Batch)
    if model_type == "keyword":
        all_categories = [detect_relief_keyword(t) for t in texts]
    else:
        all_categories = detect_relief_ai_batch(texts)

    # 2. Get Sentiments (Batch)
    all_sentiments = get_sentiments_batch(texts, model_type)

    stats = {}
    # Structure: stats[date][category] = {positive, negative, neutral}

    for date, cats, senti in zip(dates, all_categories, all_sentiments):
        if date not in stats:
            stats[date] = {}

        for cat in cats:
            if cat not in stats[date]:
                stats[date][cat] = {"positive": 0, "negative": 0, "neutral": 0}
            stats[date][cat][senti] += 1

    # 3. Flatten for API response
    result = []
    for date, cat_dict in stats.items():
        for cat, counts in cat_dict.items():
            result.append({
                "date": date,
                "category": cat,
                "positive": counts["positive"],
                "negative": counts["negative"],
                "neutral": counts["neutral"]
            })
            
    return result
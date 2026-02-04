import json
from transformers import pipeline
from config import DAMAGE_CONFIG

# --- 1. KEYWORD SETUP ---
try:
    with open(DAMAGE_CONFIG, "r", encoding="utf-8") as f:
        DAMAGE_KEYWORDS = json.load(f)
        CANDIDATE_LABELS = list(DAMAGE_KEYWORDS.keys())
        if "Other" in CANDIDATE_LABELS:
            CANDIDATE_LABELS.remove("Other")
except Exception as e:
    print(f"❌ Error loading damage config: {e}")
    DAMAGE_KEYWORDS = {}
    CANDIDATE_LABELS = []

# --- 2. AI MODEL SETUP ---
print("⏳ Loading Damage AI Model...")
try:
    classifier = pipeline(
        "zero-shot-classification", 
        model="MoritzLaurer/mDeBERTa-v3-base-mnli-xnli",
        device=-1 
    )
    print("✅ Damage AI Model Loaded.")
except Exception as e:
    print(f"❌ AI Load Failed: {e}")
    classifier = None

# --- CORE FUNCTIONS ---

def classify_damage_keyword_batch(texts: list) -> list:
    """Fast dictionary-based matching"""
    results = []
    for text in texts:
        text_lower = text.lower()
        found = False
        for category, keywords in DAMAGE_KEYWORDS.items():
            for kw in keywords:
                if kw in text_lower:
                    results.append(category)
                    found = True
                    break
            if found: break
        if not found:
            results.append("Other")
    return results

def classify_damage_ai_batch(texts: list) -> list:
    """Accurate AI matching"""
    if classifier is None:
        return classify_damage_keyword_batch(texts) # Fallback

    results = []
    # Process in chunks of 8 to prevent memory overflow
    batch_size = 8
    for i in range(0, len(texts), batch_size):
        batch_texts = texts[i:i + batch_size]
        try:
            predictions = classifier(batch_texts, CANDIDATE_LABELS, truncation=True)
            for pred in predictions:
                if pred['scores'][0] < 0.4:
                    results.append("Other")
                else:
                    results.append(pred['labels'][0])
        except Exception as e:
            print(f"AI Batch Error: {e}")
            results.extend(["Other"] * len(batch_texts))
            
    return results

def classify_damage_batch(texts: list, model_type: str = "ai") -> list:
    if model_type == "keyword":
        print("⚡ Using Keyword Mode for Damage")
        return classify_damage_keyword_batch(texts)
    else:
        print("🧠 Using AI Mode for Damage")
        return classify_damage_ai_batch(texts)
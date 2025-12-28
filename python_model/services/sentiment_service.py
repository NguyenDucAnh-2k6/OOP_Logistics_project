from config import SENTIMENT_CONFIG
import json
from typing import List,Dict
with open(SENTIMENT_CONFIG, "r", encoding="utf-8") as f:
    cfg = json.load(f)

POSITIVE_WORDS = cfg["positive"]
NEGATIVE_WORDS = cfg["negative"]

def predict_sentiment(text: str) -> str:
    if not text:
        return "neutral"

    text_lower = text.lower()
    score = 0

    for w in POSITIVE_WORDS:
        if w in text_lower:
            score += 1
    for w in NEGATIVE_WORDS:
        if w in text_lower:
            score -= 1

    if score > 0:
        return "positive"
    elif score < 0:
        return "negative"
    return "neutral"
def aggregate_by_date(texts: List[str], dates: List[str]) -> List[Dict]:
    """
    Gom nhóm sentiment theo ngày cho bài toán 1.
    Input:
        texts: danh sách text
        dates: cùng độ dài với texts, 'YYYY-MM-DD'
    Output:
        list[ { "date": ..., "positive": x, "negative": y, "neutral": z } ]
    """
    if len(texts) != len(dates):
        raise ValueError("texts và dates phải có cùng số lượng phần tử")

    stats: Dict[str, Dict[str, int]] = {}

    for text, date in zip(texts, dates):
        senti = predict_sentiment(text)
        if date not in stats:
            stats[date] = {"positive": 0, "negative": 0, "neutral": 0}
        stats[date][senti] += 1

    result = []
    # Chuyển về list để dễ serialize JSON
    for d, v in stats.items():
        result.append({
            "date": d,
            "positive": v["positive"],
            "negative": v["negative"],
            "neutral": v["neutral"],
        })
    return result
from typing import List, Dict

from .sentiment_service import predict_sentiment

from config import RELIEF_CONFIG
import json

with open(RELIEF_CONFIG, "r", encoding="utf-8") as f:
    RELIEF_KEYWORDS = json.load(f)

def detect_relief_categories(text: str) -> List[str]:
    if not text:
        return ["Other"]

    text_lower = text.lower()
    cats = []
    for cat, kws in RELIEF_KEYWORDS.items():
        if any(kw in text_lower for kw in kws):
            cats.append(cat)

    return cats if cats else ["Other"]
def aggregate_relief_sentiment(texts: List[str]) -> Dict[str, Dict[str, int]]:
    """
    Bài toán 3: Gom sentiment theo từng loại hàng cứu trợ (không theo thời gian).
    Output: {
      "Food": {"positive": x, "negative": y, "neutral": z},
      "Housing": {...},
      ...
    }
    """
    stats: Dict[str, Dict[str, int]] = {}

    for text in texts:
        senti = predict_sentiment(text)
        cats = detect_relief_categories(text)

        for cat in cats:
            if cat not in stats:
                stats[cat] = {"positive": 0, "negative": 0, "neutral": 0}
            stats[cat][senti] += 1

    return stats


def aggregate_relief_time_series(texts: List[str], dates: List[str]):
    """
    Bài toán 4: Gom theo ngày + loại hàng cứu trợ.
    Output: list[
      {
        "date": "...",
        "category": "Food",
        "positive": ...,
        "negative": ...,
        "neutral": ...
      },
      ...
    ]
    """
    if len(texts) != len(dates):
        raise ValueError("texts và dates phải có cùng số lượng phần tử")

    stats: Dict[str, Dict[str, Dict[str, int]]] = {}
    # stats[date][category] = {positive, negative, neutral}

    for text, date in zip(texts, dates):
        senti = predict_sentiment(text)
        cats = detect_relief_categories(text)

        if date not in stats:
            stats[date] = {}

        for cat in cats:
            if cat not in stats[date]:
                stats[date][cat] = {"positive": 0, "negative": 0, "neutral": 0}
            stats[date][cat][senti] += 1

    result = []
    for date, cat_map in stats.items():
        for cat, val in cat_map.items():
            result.append({
                "date": date,
                "category": cat,
                "positive": val["positive"],
                "negative": val["negative"],
                "neutral": val["neutral"],
            })
    return result
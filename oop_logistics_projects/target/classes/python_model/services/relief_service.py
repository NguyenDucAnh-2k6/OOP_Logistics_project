from typing import List, Dict

from .sentiment_service import predict_sentiment

# Từ khóa cho từng loại hàng cứu trợ
RELIEF_KEYWORDS = {
    "Food_Water": [
        "mì tôm", "nước sạch", "nước uống", "lương thực", 
        "thực phẩm", "gạo", "cơm", "bánh chưng", "đồ ăn"
    ],
    "Medical": [
        "thuốc", "bác sĩ", "y tế", "cấp cứu", "bệnh viện", 
        "khám", "chữa trị", "sơ cứu"
    ],
    "Shelter": [
        "chỗ ở", "trú ẩn", "lánh nạn", "nhà văn hóa", 
        "trường học", "nhà bạt", "tấm lợp"
    ],
    "Transportation": [ # Rescue & Moving
        "xuồng", "thuyền", "cano", "áo phao", "di chuyển",
        "đón", "xe", "thông đường"
    ],
    "Cash_Financial": [
        "tiền", "ủng hộ", "quyên góp", "ngân hàng", "chuyển khoản",
        "tài trợ", "kinh phí"
    ],
    "Utilities": [ # Restoration of services (Highly relevant for satisfaction)
        "điện lực", "thợ điện", "có điện lại", "nối dây", 
        "khôi phục", "viettel", "vinaphone", "mạng"
    ]
}


def detect_relief_categories(text: str) -> List[str]:
    """
    Xác định text này đang nói về loại hàng cứu trợ nào.
    Có thể thuộc nhiều category cùng lúc.
    Nếu không khớp gì → trả về ["Other"].
    """
    if text is None:
        return ["Other"]

    text_lower = text.lower()
    categories = []

    for cat, kws in RELIEF_KEYWORDS.items():
        for kw in kws:
            if kw in text_lower:
                categories.append(cat)
                break

    if not categories:
        return ["Other"]
    return categories


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

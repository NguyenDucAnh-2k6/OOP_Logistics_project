from typing import List, Dict

POSITIVE_WORDS = [
    # Hope & Safety (Common in your CSV)
    "bình an", "cầu mong", "mong", "nguyện cầu", "không sao", 
    "qua nhanh", "nhẹ nhàng", "may mắn", "đỡ khổ", "yên tâm",
    
    # Relief & Support
    "hỗ trợ", "cứu hộ", "giúp đỡ", "kịp thời", "cảm ơn", 
    "ấm lòng", "tình người", "đoàn kết", "an toàn"
]

NEGATIVE_WORDS = [
    # Fear & Horror
    "khủng khiếp", "sợ", "khiếp", "kinh", "dã man", "sợ hãi", 
    "lo quá", "bàng hoàng", "xót xa", "đau xót", "ghê", "ác",
    
    # Damage & Loss descriptions
    "tan hoang", "tàn phá", "thiệt hại", "mất mát", "thương tâm",
    "hư hỏng", "đổ nát", "chia cắt", "cô lập", "kêu cứu",
    "mất điện", "mất nước", "mất sóng", "không liên lạc"
]

# ... rest of your file (predict_sentiment function etc.) ...


def predict_sentiment(text: str) -> str:
    """
    Dự đoán sentiment cho 1 đoạn text:
    - 'positive'
    - 'negative'
    - 'neutral'
    """
    if text is None:
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
    else:
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

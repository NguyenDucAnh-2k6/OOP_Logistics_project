from typing import List, Dict

POSITIVE_WORDS = [
    # General positive terms
    "cảm ơn", "tốt", "ổn", "an toàn", "được hỗ trợ",
    "hài lòng", "đầy đủ", "cứu trợ", "giúp đỡ", "tuyệt vời",
    "may mắn", "kịp thời", "ấm lòng", 
    
    # Extracted from YagiComments.csv (Wishes for safety, relief)
    "bình an", "cầu mong", "mong", "không sao", "qua nhanh",
    "không thiệt hại", "đỡ khổ"
]

NEGATIVE_WORDS = [
    # General negative terms
    "hư hỏng", "tắc", "thiệt hại", "không có", "mất điện",
    "quá chậm", "bất tiện", "không nhận", "thiếu", "bỏ sót",
    
    # Extracted from YagiComments.csv (Fear, Damage, Loss)
    "khủng khiếp", "sợ", "khiếp", "kinh", "dã man", "sợ hãi",
    "nguy hiểm", "mất sóng", "không liên lạc", "mất nước",
    "tan hoang", "đổ", "gãy", "gẫy", "bay", "tốc mái",
    "tàn phá", "bật gốc", "ngập", "xót xa", "mất mát",
    "tan tác", "sập", "hỏng hết", "lật thuyền", "gió giật", "ác"
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

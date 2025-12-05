from typing import List

# Map từ khóa → loại thiệt hại
DAMAGE_KEYWORDS = {
    "HouseDamage": ["nhà", "mái", "tường", "sập", "ngập nhà", "tốc mái", "tốc"],
    "InfrastructureDamage": ["cầu", "đường", "đê", "trụ điện", "cột điện", "cống"],
    "HumanAffected": ["người chết", "thương vong", "mất tích", "bị thương"],
    "EconomicDisruption": ["kinh tế", "sản xuất", "buôn bán", "nhà máy", "xí nghiệp"],
    "PropertyLost": ["mất", "tài sản", "đồ đạc", "xe", "xe máy"],
}


def classify_damage(text: str) -> str:
    """
    Phân loại loại thiệt hại cho 1 đoạn text.
    Nếu không khớp từ khóa nào → trả về 'Other'.
    """
    if text is None:
        return "Other"

    text_lower = text.lower()
    for label, kws in DAMAGE_KEYWORDS.items():
        for kw in kws:
            if kw in text_lower:
                return label
    return "Other"


def classify_damage_batch(texts: List[str]) -> List[str]:
    """
    Phân loại batch nhiều text cùng lúc.
    """
    return [classify_damage(t) for t in texts]

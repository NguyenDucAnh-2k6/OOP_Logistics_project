from typing import List

# Map từ khóa → loại thiệt hại
DAMAGE_KEYWORDS = {
    "Infrastructure": [ # Power, Roads, Telecom
        "mất điện", "cắt điện", "trạm điện", "cột điện", "dây điện",
        "mất sóng", "mất mạng", "không có sóng", "liên lạc",
        "tắc đường", "ngập", "chia cắt", "sạt lở", "cầu", "cống"
    ],
    "Housing": [ # Houses, Buildings
        "tốc mái", "bay mái", "sập", "đổ tường", "vỡ kính",
        "bay cửa", "hư hỏng nhà", "trần nhà", "ngập vào nhà",
        "nhà cửa", "tan hoang"
    ],
    "Environment": [ # Trees, Crops (Very common in Yagi comments)
        "cây đổ", "gãy cây", "bật gốc", "cây xanh", "đổ ngổn ngang",
        "hoa màu", "lúa", "ngập úng"
    ],
    "People": [ # Human impact
        "bị thương", "tử vong", "mất tích", "người dân", 
        "lật thuyền", "nạn nhân", "kêu cứu", "mắc kẹt"
    ],
    "Economic": [ # Business, Assets
        "biển quảng cáo", "biển cửa hàng", "nhà xưởng", "kho",
        "ô tô", "xe máy", "tài sản"
    ]
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

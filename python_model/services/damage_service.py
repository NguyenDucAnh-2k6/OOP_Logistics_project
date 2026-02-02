from typing import List
import json
from typing import List
from config import DAMAGE_CONFIG

with open(DAMAGE_CONFIG, "r", encoding="utf-8") as f:
    DAMAGE_KEYWORDS = json.load(f)

def classify_damage(text: str) -> str:
    if not text:
        return "Other"

    text_lower = text.lower()
    for label, kws in DAMAGE_KEYWORDS.items():
        for kw in kws:
            if kw in text_lower:
                return label
    return "Other"

def classify_damage_batch(texts: List[str]) -> List[str]:
    return [classify_damage(t) for t in texts]
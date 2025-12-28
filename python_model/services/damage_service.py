from typing import List, Dict
import json
from config import DAMAGE_CONFIG

with open(DAMAGE_CONFIG, "r", encoding="utf-8") as f:
    DAMAGE_KEYWORDS = json.load(f)

def classify_damage(text: str) -> List[str]:
    """
    Classify text into damage categories.
    Returns ALL matching categories (not just the first one).
    
    Logic: If a word/text matches keywords from multiple categories,
    ALL those categories get +1 mention.
    
    Args:
        text: Input text to classify
    
    Returns:
        List of category names that match
    """
    if not text:
        return ["Other"]

    text_lower = text.lower()
    matched_categories = []
    
    # Check each category and collect ALL matches
    for label, kws in DAMAGE_KEYWORDS.items():
        for kw in kws:
            if kw in text_lower:
                matched_categories.append(label)
                break  # Only add category once per text, even if multiple keywords match
    
    # If no categories matched, return "Other"
    if not matched_categories:
        return ["Other"]
    
    return matched_categories

def classify_damage_batch(texts: List[str]) -> List[List[str]]:
    """
    Classify a batch of texts.
    
    Returns:
        List of lists, where each inner list contains all matching categories for that text
    """
    return [classify_damage(t) for t in texts]

def get_damage_counts(texts: List[str]) -> Dict[str, int]:
    """
    Get total mention counts for each damage category.
    If a text matches multiple categories, each category gets +1.
    
    Args:
        texts: List of texts to analyze
    
    Returns:
        Dictionary mapping category name to mention count
    """
    counts = {}
    
    # Classify all texts
    all_classifications = classify_damage_batch(texts)
    
    # Count mentions for each category
    for categories in all_classifications:
        for category in categories:
            counts[category] = counts.get(category, 0) + 1
    
    return counts

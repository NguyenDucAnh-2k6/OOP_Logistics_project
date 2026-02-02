# Damage Classification Logic Fix - Analysis & Solution

## Problem Identified

The current implementation in `damage_service.py` used **"first match"** logic instead of **"count all matches"** logic.

### Current (INCORRECT) Behavior:
```python
def classify_damage(text: str) -> str:
    for label, kws in DAMAGE_KEYWORDS.items():
        for kw in kws:
            if kw in text_lower:
                return label  # ❌ Returns immediately on FIRST match
    return "Other"
```

**Issue**: If a text contains keywords from multiple categories (e.g., both "mất điện" from Infrastructure AND "bị thương" from People), it only counts as one category - whichever matches first in the dictionary.

This caused **missing categories** in the Damage Classification chart on the UI because many texts matched multiple damage types but were only counted once.

---

## Solution Implemented

Updated the logic to **count all matching categories**:

### New (CORRECT) Behavior:
```python
def classify_damage(text: str) -> List[str]:
    """
    Returns ALL matching categories for a text.
    If a word belongs to multiple categories, increment ALL of them.
    """
    matched_categories = []
    
    for label, kws in DAMAGE_KEYWORDS.items():
        for kw in kws:
            if kw in text_lower:
                matched_categories.append(label)
                break  # Add category once per text
    
    return matched_categories if matched_categories else ["Other"]
```

---

## Changes Made

### 1. **damage_service.py** - Complete refactor
- **Old**: `classify_damage(text) -> str` (returns single category)
- **New**: `classify_damage(text) -> List[str]` (returns all matching categories)
- **New**: `get_damage_counts(texts) -> Dict[str, int]` (counts mentions per category)
- Added comprehensive documentation

### 2. **main.py** - Updated API endpoint
```python
@app.post("/damage-classification")
def damage_classification(req: DamageRequest):
    return {
        "counts": counts,           # {category: mention_count, ...}
        "classifications": classifications  # [[cat1, cat2, ...], ...]
    }
```

### 3. **plotting_234.py** & **analyze_and_plot.py** - Updated plot handlers
- Changed to extract `counts` from response instead of counting strings
- Updated chart title to clarify: "Each category gets +1 if text mentions multiple damages"

---

## Expected Results

### Before Fix:
- Text: "mất điện nhưng người dân bị thương" → Counted as 1 category (Infrastructure)
- UI showed incomplete damage breakdown (missing People category)

### After Fix:
- Text: "mất điện nhưng người dân bị thương" → Counted as 2 categories (Infrastructure + People)
- UI now shows **all damage categories mentioned** with accurate counts
- Chart now includes categories that may have been previously invisible

---

## Example Output

**API Response Format:**
```json
{
  "counts": {
    "Infrastructure": 45,
    "Housing": 32,
    "People": 28,
    "Environment": 15,
    "Economic": 12
  },
  "classifications": [
    ["Infrastructure"],
    ["Infrastructure", "People"],
    ["Housing"],
    ...
  ]
}
```

---

## Files Modified
1. `python_model/services/damage_service.py` - Core logic fix
2. `python_model/main.py` - API endpoint update
3. `python_model/plotting_234.py` - Plot update
4. `python_model/analyze_and_plot.py` - Plot update

## Testing Recommendations
- [ ] Run with test data containing multi-category mentions
- [ ] Verify all damage categories appear in UI chart
- [ ] Check that category mention counts are higher than text count (due to overlaps)
- [ ] Ensure "Other" is properly excluded from main categories

from pydantic import BaseModel, Field, model_validator
from typing import List, Optional, Literal

class BaseRequest(BaseModel):
    # Field(...) makes it required and adds documentation
    texts: List[str] = Field(..., description="List of text contents (posts/comments)")
    
    # Literal strictly enforces that ONLY "ai" or "keyword" can be accepted
    model_type: Literal["ai", "keyword"] = Field("ai", description="Analysis engine to use")

class SentimentTimeSeriesRequest(BaseRequest):
    dates: List[str] = Field(..., description="List of dates matching the texts ('DD/MM/YYYY' or 'YYYY-MM-DD')")

    # This ensures Java doesn't send mismatched arrays
    @model_validator(mode='after')
    def check_lengths_match(self) -> 'SentimentTimeSeriesRequest':
        if len(self.texts) != len(self.dates):
            raise ValueError(f"Length mismatch: {len(self.texts)} texts vs {len(self.dates)} dates.")
        return self

class DamageRequest(BaseRequest):
    """Inherits 'texts' and 'model_type' directly from BaseRequest."""
    pass 

class ReliefSentimentRequest(BaseRequest):
    dates: Optional[List[str]] = Field(None, description="Optional dates for time-series relief analysis")

    @model_validator(mode='after')
    def check_lengths_match_if_dates_provided(self) -> 'ReliefSentimentRequest':
        if self.dates is not None and len(self.texts) != len(self.dates):
            raise ValueError(f"Length mismatch: {len(self.texts)} texts vs {len(self.dates)} dates.")
        return self
class IntentRequest(BaseRequest):
    """
    Request for Problem 5: Supply vs. Demand Classification.
    Inherits 'texts' and 'model_type' from BaseRequest.
    """
    pass
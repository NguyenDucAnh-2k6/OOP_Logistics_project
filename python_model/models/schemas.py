from pydantic import BaseModel
from typing import List, Optional

class BaseRequest(BaseModel):
    texts: List[str]
    model_type: str = "ai"  # Options: "ai" (default), "keyword"
class SentimentTimeSeriesRequest(BaseRequest):
    """
    Request cho bài toán 1: sentiment theo thời gian.
    - texts: danh sách bài post/comment
    - dates: danh sách ngày tương ứng với từng text (chuỗi 'YYYY-MM-DD')
    """
    texts: List[str]
    dates: List[str]


class DamageRequest(BaseRequest):
    """
    Request cho bài toán 2: phân loại loại thiệt hại.
    - texts: danh sách bài post/comment mô tả thiệt hại
    """
    texts: List[str]


class ReliefSentimentRequest(BaseRequest):
    """
    Request cho bài toán 3 + 4: phân tích hàng cứu trợ.
    - texts: danh sách bài post/comment
    - dates: chỉ cần cho bài toán 4 (time-series), có thể None cho bài toán 3
    """
    texts: List[str]
    dates: Optional[List[str]] = None
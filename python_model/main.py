from fastapi import FastAPI, Request

from models.schemas import (
    SentimentTimeSeriesRequest,
    DamageRequest,
    ReliefSentimentRequest,
)
from services.sentiment_service import aggregate_by_date
from services.damage_service import classify_damage_batch
from services.relief_service import (
    aggregate_relief_sentiment,
    aggregate_relief_time_series,
)

app = FastAPI(title="Humanitarian Logistics Analysis API")


@app.post("/sentiment-time-series")
def sentiment_time_series(req: SentimentTimeSeriesRequest):
    """
    Bài toán 1: Theo dõi tâm lý công chúng theo thời gian.
    Input: danh sách texts + dates
    Output: list[ {date, positive, negative, neutral} ]
    """
    print(">>> ĐÃ NHẬN REQ SENTIMENT TIME SERIES")
    print(req.model_dump())
    return aggregate_by_date(req.texts, req.dates)


@app.post("/damage-classification")
def damage_classification(req: DamageRequest):
    """
    Bài toán 2: Xác định loại thiệt hại phổ biến.
    Input: danh sách texts
    Output: list[ "HouseDamage", "InfrastructureDamage", ... ]
    """
    return classify_damage_batch(req.texts)


@app.post("/relief-sentiment")
def relief_sentiment(req: ReliefSentimentRequest):
    """
    Bài toán 3: Mức độ hài lòng / không hài lòng theo từng loại hàng cứu trợ.
    Input: texts
    Output: {
      "Food": {"positive": x, "negative": y, "neutral": z},
      ...
    }
    """
    return aggregate_relief_sentiment(req.texts)


@app.post("/relief-time-series")
def relief_time_series(req: ReliefSentimentRequest):
    """
    Bài toán 4: Tâm lý theo từng loại hàng cứu trợ theo thời gian.
    Yêu cầu có `dates`.
    Output: list[ {date, category, positive, negative, neutral} ]
    """
    if req.dates is None:
        raise ValueError("Trường 'dates' là bắt buộc cho API /relief-time-series")
    return aggregate_relief_time_series(req.texts, req.dates)
@app.middleware("http")
async def log_requests(request: Request, call_next):
    body = await request.body()
    print("\n========== RAW REQUEST BODY ==========")
    print(body[:500], "...")   # tránh in quá dài
    print("=====================================\n")
    response = await call_next(request)
    return response

# Chạy trực tiếp bằng: python main.py
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
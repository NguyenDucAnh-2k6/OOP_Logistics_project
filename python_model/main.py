from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
import uvicorn

# Import request models
from models.schemas import (
    SentimentTimeSeriesRequest,
    DamageRequest,
    ReliefSentimentRequest,
    IntentRequest
)

# Import service logic
from services.sentiment_service import aggregate_by_date
from services.damage_service import classify_damage_batch
from services.relief_service import (
    aggregate_relief_sentiment,
    aggregate_relief_time_series,
)
from services.intent_service import aggregate_intent_stats

app = FastAPI(title="Humanitarian Logistics Analysis API")

# --- ROOT CHECK ---
@app.get("/")
def health_check():
    return {"status": "running", "message": "Python Analysis Backend is Active"}

# --- 1. SENTIMENT TIME SERIES ---
# Java sends: /analyze/sentiment_timeseries
@app.post("/analyze/sentiment_timeseries")
def analyze_sentiment_timeseries(req: SentimentTimeSeriesRequest):
    """
    Problem 1: Sentiment trend over time.
    """
    print(f"📊 [Problem 1] Sentiment Request ({req.model_type})")
    try:
        return aggregate_by_date(req.texts, req.dates, req.model_type)
    except Exception as e:
        print(f"❌ Error: {e}")
        return JSONResponse(status_code=500, content={"error": str(e)})

# --- 2. DAMAGE CLASSIFICATION ---
# Java sends: /analyze/damage
@app.post("/analyze/damage")
def analyze_damage(req: DamageRequest):
    """
    Problem 2: Classify damage types.
    """
    print(f"🏚️ [Problem 2] Damage Request ({req.model_type})")
    try:
        return classify_damage_batch(req.texts, req.model_type)
    except Exception as e:
        print(f"❌ Error: {e}")
        return JSONResponse(status_code=500, content={"error": str(e)})

# --- 3. RELIEF SENTIMENT ---
# Java sends: /analyze/relief_sentiment
@app.post("/analyze/relief_sentiment")
def analyze_relief_sentiment(req: ReliefSentimentRequest):
    """
    Problem 3: Sentiment by relief category.
    """
    print(f"💊 [Problem 3] Relief Sentiment Request ({req.model_type})")
    try:
        return aggregate_relief_sentiment(req.texts, req.model_type)
    except Exception as e:
        print(f"❌ Error: {e}")
        return JSONResponse(status_code=500, content={"error": str(e)})

# --- 4. RELIEF TIME SERIES ---
# Java sends: /analyze/relief_timeseries
@app.post("/analyze/relief_timeseries")
def analyze_relief_timeseries(req: ReliefSentimentRequest):
    """
    Problem 4: Relief needs over time.
    """
    print(f"📈 [Problem 4] Relief Trend Request ({req.model_type})")
    if req.dates is None:
        return JSONResponse(status_code=400, content={"error": "Dates are required for time-series analysis"})
        
    try:
        return aggregate_relief_time_series(req.texts, req.dates, req.model_type)
    except Exception as e:
        print(f"❌ Error: {e}")
        return JSONResponse(status_code=500, content={"error": str(e)})
@app.post("/analyze/intent")
def analyze_intent(req: IntentRequest):
    """
    Problem 5: Classify Supply (Offer) vs Demand (Request).
    """
    print(f"🤝 [Problem 5] Intent Request ({req.model_type})")
    try:
        return aggregate_intent_stats(req.texts, req.model_type)
    except Exception as e:
        print(f"❌ Error: {e}")
        return JSONResponse(status_code=500, content={"error": str(e)})
# --- STARTUP ---
if __name__ == "__main__":
    print("🚀 Starting Python Backend on Port 8000...")
    uvicorn.run(app, host="127.0.0.1", port=8000)
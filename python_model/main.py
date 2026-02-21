from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
import uvicorn
import logging

# Setup logging first
from logging_config import logger

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
    logger.info("Health check request received")
    return {"status": "running", "message": "Python Analysis Backend is Active"}

# --- 1. SENTIMENT TIME SERIES ---
# Java sends: /analyze/sentiment_timeseries
@app.post("/analyze/sentiment_timeseries")
def analyze_sentiment_timeseries(req: SentimentTimeSeriesRequest):
    """
    Problem 1: Sentiment trend over time.
    """
    logger.info(f"[Problem 1] Sentiment Request (model_type={req.model_type}, texts_count={len(req.texts)})")
    try:
        result = aggregate_by_date(req.texts, req.dates, req.model_type)
        logger.info(f"[Problem 1] Sentiment analysis completed successfully")
        return result
    except Exception as e:
        logger.error(f"[Problem 1] Error during sentiment analysis: {e}", exc_info=True)
        return JSONResponse(status_code=500, content={"error": str(e)})

# --- 2. DAMAGE CLASSIFICATION ---
# Java sends: /analyze/damage
@app.post("/analyze/damage")
def analyze_damage(req: DamageRequest):
    """
    Problem 2: Classify damage types.
    """
    logger.info(f"[Problem 2] Damage Request (model_type={req.model_type}, texts_count={len(req.texts)})")
    try:
        result = classify_damage_batch(req.texts, req.model_type)
        logger.info(f"[Problem 2] Damage classification completed successfully")
        return result
    except Exception as e:
        logger.error(f"[Problem 2] Error during damage classification: {e}", exc_info=True)
        return JSONResponse(status_code=500, content={"error": str(e)})

# --- 3. RELIEF SENTIMENT ---
# Java sends: /analyze/relief_sentiment
@app.post("/analyze/relief_sentiment")
def analyze_relief_sentiment(req: ReliefSentimentRequest):
    """
    Problem 3: Aggregate relief sentiment.
    """
    logger.info(f"[Problem 3] Relief Sentiment Request (model_type={req.model_type}, texts_count={len(req.texts)})")
    try:
        result = aggregate_relief_sentiment(req.texts, req.model_type)
        logger.info(f"[Problem 3] Relief sentiment analysis completed successfully")
        return result
    except Exception as e:
        logger.error(f"[Problem 3] Error during relief sentiment analysis: {e}", exc_info=True)
        return JSONResponse(status_code=500, content={"error": str(e)})

# --- 4. RELIEF TIME SERIES ---
# Java sends: /analyze/relief_timeseries
@app.post("/analyze/relief_timeseries")
def analyze_relief_timeseries(req: ReliefSentimentRequest):
    """
    Problem 4: Relief needs over time.
    """
    logger.info(f"[Problem 4] Relief Trend Request (model_type={req.model_type})")
    if req.dates is None:
        return JSONResponse(status_code=400, content={"error": "Dates are required for time-series analysis"})
        
    try:
        result = aggregate_relief_time_series(req.texts, req.dates, req.model_type)
        logger.info(f"[Problem 4] Relief time-series analysis completed successfully")
        return result
    except Exception as e:
        logger.error(f"[Problem 4] Error during relief time-series analysis: {e}", exc_info=True)
        return JSONResponse(status_code=500, content={"error": str(e)})

# --- 5. INTENT CLASSIFICATION ---
# Java sends: /analyze/intent
@app.post("/analyze/intent")
def analyze_intent(req: IntentRequest):
    """
    Problem 5: Classify Supply (Offer) vs Demand (Request).
    """
    logger.info(f"[Problem 5] Intent Request (model_type={req.model_type})")
    try:
        result = aggregate_intent_stats(req.texts, req.model_type)
        logger.info(f"[Problem 5] Intent analysis completed successfully")
        return result
    except Exception as e:
        logger.error(f"[Problem 5] Error during intent analysis: {e}", exc_info=True)
        return JSONResponse(status_code=500, content={"error": str(e)})

# --- STARTUP ---
if __name__ == "__main__":
    logger.info("🚀 Starting Python Backend on Port 8000...")
    uvicorn.run(app, host="127.0.0.1", port=8000)
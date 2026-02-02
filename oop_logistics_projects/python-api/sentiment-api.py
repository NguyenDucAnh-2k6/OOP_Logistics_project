"""
Python Sentiment Analysis API
Flask-based REST API for Vietnamese sentiment analysis
Can be easily swapped with other models or services
"""

from flask import Flask, request, jsonify
from transformers import pipeline
import time
import logging

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

# Load sentiment analysis model
# You can replace this with any other model
try:
    # Using a multilingual sentiment model
    sentiment_analyzer = pipeline(
        "sentiment-analysis",
        model="nlptown/bert-base-multilingual-uncased-sentiment"
    )
    MODEL_LOADED = True
except Exception as e:
    logging.error(f"Failed to load model: {e}")
    MODEL_LOADED = False
    sentiment_analyzer = None


def analyze_text(text, language="vi"):
    """
    Analyze sentiment of a single text
    Returns: dict with sentiment, confidence, and scores
    """
    if not text or not MODEL_LOADED:
        return {
            "sentiment": "neutral",
            "confidence": 0.0,
            "scores": {}
        }
    
    try:
        result = sentiment_analyzer(text[:512])[0]  # Limit to 512 chars
        
        # Convert model output to our format
        label = result['label']
        score = result['score']
        
        # Map ratings to sentiment
        if '5 stars' in label or '4 stars' in label:
            sentiment = "positive"
        elif '1 star' in label or '2 stars' in label:
            sentiment = "negative"
        else:
            sentiment = "neutral"
        
        return {
            "sentiment": sentiment,
            "confidence": score,
            "scores": {
                "positive": score if sentiment == "positive" else 0.0,
                "negative": score if sentiment == "negative" else 0.0,
                "neutral": score if sentiment == "neutral" else 0.0
            }
        }
    
    except Exception as e:
        logging.error(f"Analysis error: {e}")
        return {
            "sentiment": "neutral",
            "confidence": 0.0,
            "scores": {},
            "error": str(e)
        }


@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        "status": "healthy" if MODEL_LOADED else "degraded",
        "model_loaded": MODEL_LOADED
    }), 200


@app.route('/analyze', methods=['POST'])
def analyze():
    """
    Main analysis endpoint
    
    Request body:
    {
        "text": "Single text to analyze",
        "texts": ["Multiple", "texts", "to analyze"],
        "language": "vi",
        "model": "default"
    }
    
    Response:
    {
        "sentiment": "positive/negative/neutral",
        "confidence": 0.85,
        "scores": {"positive": 0.85, "negative": 0.10, "neutral": 0.05},
        "results": [...],  // for batch processing
        "processing_time": 123
    }
    """
    start_time = time.time()
    
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({"error": "No JSON data provided"}), 400
        
        # Single text analysis
        if 'text' in data:
            text = data['text']
            language = data.get('language', 'vi')
            
            result = analyze_text(text, language)
            result['processing_time'] = int((time.time() - start_time) * 1000)
            
            return jsonify(result), 200
        
        # Batch text analysis
        elif 'texts' in data:
            texts = data['texts']
            language = data.get('language', 'vi')
            
            results = []
            for text in texts:
                analysis = analyze_text(text, language)
                results.append({
                    "text": text[:100],  # Include truncated text
                    "sentiment": analysis['sentiment'],
                    "confidence": analysis['confidence']
                })
            
            response = {
                "results": results,
                "processing_time": int((time.time() - start_time) * 1000)
            }
            
            return jsonify(response), 200
        
        else:
            return jsonify({"error": "Must provide 'text' or 'texts' field"}), 400
    
    except Exception as e:
        logging.error(f"Request error: {e}")
        return jsonify({
            "error": str(e),
            "processing_time": int((time.time() - start_time) * 1000)
        }), 500


@app.route('/models', methods=['GET'])
def list_models():
    """List available models"""
    return jsonify({
        "models": [
            {
                "name": "default",
                "description": "Multilingual BERT sentiment analysis",
                "languages": ["vi", "en", "multilingual"]
            }
        ]
    }), 200


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
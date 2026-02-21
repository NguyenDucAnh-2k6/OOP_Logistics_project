from transformers import pipeline
from threading import Lock
import logging

logger = logging.getLogger(__name__)

class ModelLoader:
    _instance = None
    _lock = Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super(ModelLoader, cls).__new__(cls)
                    cls._instance._load_models()
        return cls._instance

    def _load_models(self):
        logger.info("Loading AI Models... (This may take a while on first run)")
        
        # 1. Sentiment Model (Vietnamese PhoBERT)
        # Maps output to NEG, NEU, POS
        self.sentiment_pipeline = pipeline(
            "sentiment-analysis", 
            model="wonrax/phobert-base-vietnamese-sentiment",
            tokenizer="wonrax/phobert-base-vietnamese-sentiment"
        )

        # 2. Zero-Shot Classification (for Damage detection)
        # This model works for 100+ languages including Vietnamese
        self.zero_shot_pipeline = pipeline(
            "zero-shot-classification", 
            model="MoritzLaurer/mDeBERTa-v3-base-mnli-xnli"
        )
        logger.info("AI Models loaded successfully")

    def get_sentiment_analyzer(self):
        return self.sentiment_pipeline

    def get_zero_shot_analyzer(self):
        return self.zero_shot_pipeline

# Global instance
ai_models = ModelLoader()
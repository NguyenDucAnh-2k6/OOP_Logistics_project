import logging
import os
from logging.handlers import RotatingFileHandler

# Ensure logs directory exists
LOG_DIR = os.path.join(os.path.dirname(__file__), "..", "logs")
os.makedirs(LOG_DIR, exist_ok=True)

# Log file paths
API_LOG_FILE = os.path.join(LOG_DIR, "python-api.log")

def setup_logging():
    """Configure logging for entire Python application"""
    
    # Create logger
    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)
    
    # Remove any existing handlers
    logger.handlers.clear()
    
    # === FILE HANDLER ===
    file_handler = RotatingFileHandler(
        API_LOG_FILE,
        maxBytes=10 * 1024 * 1024,  # 10MB
        backupCount=10
    )
    file_handler.setLevel(logging.DEBUG)
    
    # === CONSOLE HANDLER ===
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    
    # === FORMATTER ===
    formatter = logging.Formatter(
        '%(asctime)s [%(name)s] %(levelname)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    
    file_handler.setFormatter(formatter)
    console_handler.setFormatter(formatter)
    
    logger.addHandler(file_handler)
    logger.addHandler(console_handler)
    
    # === SUPPRESS VERBOSE THIRD-PARTY LOGS ===
    logging.getLogger("uvicorn.access").setLevel(logging.INFO)
    logging.getLogger("uvicorn.error").setLevel(logging.INFO)
    logging.getLogger("fastapi").setLevel(logging.INFO)
    
    return logger

# Initialize logging on import
logger = setup_logging()

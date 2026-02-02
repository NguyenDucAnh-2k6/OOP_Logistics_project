import os
from pathlib import Path

CONFIG_DIR = Path(
    os.getenv(
        "OOP_LOGISTICS_CONFIG_DIR",
        r"D:\JAVAProjects\OOP_Logistics_project\oop_logistics_projects\external config"
    )
)

# Các file config cụ thể
DAMAGE_CONFIG = CONFIG_DIR / "damage_keywords.json"
RELIEF_CONFIG = CONFIG_DIR / "relief_keywords.json"
SENTIMENT_CONFIG = CONFIG_DIR / "sentiment_keywords.json"
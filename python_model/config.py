import os

# Get the absolute path of the directory where this config.py file is located
# e.g., .../oop_logistics_projects/python_model
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# Go up one level to get the project root
# e.g., .../oop_logistics_projects
PROJECT_ROOT = os.path.dirname(BASE_DIR)

# --- DATA FILES ---
# Using os.path.join ensures compatibility with both Windows and Mac/Linux
DATA_PATH = os.path.join(PROJECT_ROOT, "YagiComments.csv")
NEWS_DATA_PATH = os.path.join(PROJECT_ROOT, "YagiNews.csv")

# --- MODEL PATHS ---
MODEL_DIR = os.path.join(BASE_DIR, "models")

# --- CONFIGURATION FILES ---
# 1. We use os.path.join (strings) instead of the '/' operator (which requires pathlib).
# 2. We removed "oop_logistics_projects" from the path because PROJECT_ROOT 
#    is ALREADY inside "oop_logistics_projects".
DAMAGE_CONFIG = os.path.join(PROJECT_ROOT, "oop_logistics_projects", "external config", "damage_keywords.json")
RELIEF_CONFIG = os.path.join(PROJECT_ROOT, "oop_logistics_projects", "external config", "relief_keywords.json")
SENTIMENT_CONFIG = os.path.join(PROJECT_ROOT, "oop_logistics_projects", "external config", "sentiment_keywords.json")
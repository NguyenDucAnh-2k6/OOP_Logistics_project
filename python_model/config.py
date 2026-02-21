import os

# Get the absolute path of the directory where config.py is located
# e.g., .../OOP_Logistics_Project/python_model
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# Go up EXACTLY ONE level to get the project root
# e.g., .../OOP_Logistics_Project
PROJECT_ROOT = os.path.dirname(BASE_DIR)

# --- DATA FILES ---
# Using os.path.join ensures compatibility with both Windows and Mac/Linux
DATA_PATH = os.path.join(PROJECT_ROOT, "YagiComments.csv")
NEWS_DATA_PATH = os.path.join(PROJECT_ROOT, "YagiNews.csv")

# --- MODEL PATHS ---
MODEL_DIR = os.path.join(PROJECT_ROOT, "oop_logistics_projects", "models")

# --- CONFIGURATION FILES ---
# Since oop_logistics_projects is a sibling to python_model, we build the path from PROJECT_ROOT
CONFIG_DIR = os.path.join(PROJECT_ROOT, "oop_logistics_projects", "external config") 

SENTIMENT_CONFIG = os.path.join(CONFIG_DIR, "sentiment_keywords.json")
DAMAGE_CONFIG = os.path.join(CONFIG_DIR, "damage_keywords.json")
RELIEF_CONFIG = os.path.join(CONFIG_DIR, "relief_keywords.json")
INTENT_CONFIG = os.path.join(CONFIG_DIR, "intent_keywords.json")
import csv
import requests
import matplotlib.pyplot as plt
import pandas as pd

# 1. SETUP PATHS
CSV_PATH = "YagiComments.csv"  # Ensure this is in the same folder
API_URL = "http://localhost:8000/sentiment-time-series"

texts = []
dates = []

print(f"Reading data from {CSV_PATH}...")

# 2. READ CSV (Robustly)
try:
    with open(CSV_PATH, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            # Check for headers case-insensitively or specifically
            # Your file uses "Date" and "text"
            text_val = row.get("text") or row.get("Text") or row.get("content")
            date_val = row.get("Date") or row.get("date") or row.get("Thời gian")
            
            # Only add if both exist and are not empty
            if text_val and date_val:
                texts.append(text_val)
                dates.append(date_val)

except FileNotFoundError:
    print(f"Error: Could not find file {CSV_PATH}")
    exit()

if not texts:
    print("Error: No data found! Check CSV headers (Expecting 'Date' and 'text').")
    exit()

# 3. CALL API
payload = {
    "texts": texts,
    "dates": dates
}

print(f"Sending {len(texts)} comments to API...")
try:
    response = requests.post(API_URL, json=payload)
    response.raise_for_status()
    data = response.json() 
    print("Analysis complete!")
except requests.exceptions.ConnectionError:
    print("Error: Could not connect to API. Make sure 'python main.py' is running.")
    exit()
except Exception as e:
    print(f"API Error: {e}")
    exit()

# 4. PROCESS & PLOT DATA
df = pd.DataFrame(data)

if df.empty:
    print("API returned no results to plot.")
    exit()

# Convert 'date' to datetime, turning errors into NaT (Not a Time)
df['date'] = pd.to_datetime(df['date'], dayfirst=True, errors='coerce')

# --- CRITICAL FIX: Drop rows where date conversion failed ---
df = df.dropna(subset=['date']) 

if df.empty:
    print("No valid dates found after processing.")
    exit()

df = df.sort_values('date')

# Format date back to string
df['date_str'] = df['date'].dt.strftime('%d/%m/%Y')

# Verify we have no NaNs left in date_str (Just in case)
df = df.dropna(subset=['date_str'])

print("Plotting results...")

# Plotting
plt.figure(figsize=(10, 6))

plt.plot(df['date_str'], df['positive'], marker='o', label='Positive', color='green')
plt.plot(df['date_str'], df['negative'], marker='o', label='Negative', color='red')
plt.plot(df['date_str'], df['neutral'], marker='o', label='Neutral', color='gray')

plt.title('Sentiment Trends Over Time (Yagi Storm)')
plt.xlabel('Date')
plt.ylabel('Number of Comments')
plt.legend()
plt.grid(True)
plt.xticks(rotation=45)
plt.tight_layout()

# Show the plot
plt.show()
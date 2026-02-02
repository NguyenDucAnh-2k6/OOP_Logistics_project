import csv
import requests
import matplotlib.pyplot as plt
import pandas as pd
from collections import Counter
import numpy as np
# 1. SETUP PATHS
CSV_PATH = "YagiComments.csv"  # Ensure this is in the same folder
API_BASE_URL = "http://localhost:8000"

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
    response = requests.post(f"{API_BASE_URL}/sentiment-time-series", json=payload)
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
try:
    with open(CSV_PATH, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            # Handle variable header names
            text_val = row.get("text") or row.get("Text") or row.get("content")
            date_val = row.get("Date") or row.get("date") or row.get("Thời gian")
            
            if text_val and date_val:
                texts.append(text_val)
                dates.append(date_val)

except FileNotFoundError:
    print(f"Error: Could not find file {CSV_PATH}")
    exit()

if not texts:
    print("Error: No data found in CSV!")
    exit()

print(f"Loaded {len(texts)} comments.")

# ==============================================================================
# PROBLEM 2: Identify Common Damage Types (Bar Chart)
# ==============================================================================
def plot_problem_2():
    print("\n--- Analyzing Problem 2: Damage Types ---")
    url = f"{API_BASE_URL}/damage-classification"
    
    try:
        response = requests.post(url, json={"texts": texts})
        response.raise_for_status()
        data = response.json()
    except Exception as e:
        print(f"API Error (Prob 2): {e}")
        return

    # Extract counts from response
    # Response format: {"counts": {category: count, ...}, "classifications": [...]}
    counts = data.get("counts", {})
    
    # Remove 'Other' if you want to focus on specific damages
    if "Other" in counts:
        del counts["Other"]

    if not counts:
        print("No damage categories detected.")
        return

    labels = list(counts.keys())
    values = list(counts.values())

    plt.figure(figsize=(10, 6))
    bars = plt.bar(labels, values, color='#e74c3c') # Red for damage
    
    plt.title('Most Common Types of Damage (Problem 2)\n(Each category gets +1 if text mentions multiple damages)')
    plt.xlabel('Damage Category')
    plt.ylabel('Number of Mentions')
    plt.grid(axis='y', linestyle='--', alpha=0.7)
    
    # Add counts on top of bars
    for bar in bars:
        height = bar.get_height()
        plt.text(bar.get_x() + bar.get_width()/2., height,
                 f'{int(height)}', ha='center', va='bottom')
    
    plt.xticks(rotation=45, ha='right')
    plt.tight_layout()
    values = list(counts.values())

    plt.figure(figsize=(10, 6))
    bars = plt.bar(labels, values, color='#e74c3c') # Red for damage
    
    plt.title('Most Common Types of Damage (Problem 2)')
    plt.xlabel('Damage Category')
    plt.ylabel('Number of Reports')
    plt.grid(axis='y', linestyle='--', alpha=0.7)
    
    # Add counts on top of bars
    for bar in bars:
        height = bar.get_height()
        plt.text(bar.get_x() + bar.get_width()/2., height,
                 f'{int(height)}', ha='center', va='bottom')

    plt.show()

# ==============================================================================
# PROBLEM 3: Public Satisfaction with Relief (Stacked Bar Chart)
# ==============================================================================
def plot_problem_3():
    print("\n--- Analyzing Problem 3: Relief Satisfaction ---")
    url = f"{API_BASE_URL}/relief-sentiment"
    
    try:
        response = requests.post(url, json={"texts": texts})
        response.raise_for_status()
        data = response.json() # Dict: {Category: {positive: X, negative: Y}}
    except Exception as e:
        print(f"API Error (Prob 3): {e}")
        return

    # Prepare data for plotting
    categories = []
    pos_counts = []
    neg_counts = []

    for cat, scores in data.items():
        if cat == "Other": continue
        categories.append(cat)
        pos_counts.append(scores.get("positive", 0))
        neg_counts.append(scores.get("negative", 0))

    if not categories:
        print("No relief categories detected.")
        return

    x = np.arange(len(categories))
    width = 0.6

    plt.figure(figsize=(10, 6))
    
    # Stacked Bar Chart
    plt.bar(x, pos_counts, width, label='Positive', color='#2ecc71')
    plt.bar(x, neg_counts, width, bottom=pos_counts, label='Negative', color='#e74c3c')

    plt.title('Public Satisfaction by Relief Item (Problem 3)')
    plt.xlabel('Relief Category')
    plt.ylabel('Sentiment Count')
    plt.xticks(x, categories)
    plt.legend()
    plt.grid(axis='y', linestyle='--', alpha=0.7)

    plt.show()

# ==============================================================================
# PROBLEM 4: Relief Effectiveness Over Time (Multi-Line Chart)
# ==============================================================================
def plot_problem_4():
    print("\n--- Analyzing Problem 4: Relief Trends Over Time ---")
    url = f"{API_BASE_URL}/relief-time-series"
    
    try:
        response = requests.post(url, json={"texts": texts, "dates": dates})
        response.raise_for_status()
        data = response.json() # List of dicts
    except Exception as e:
        print(f"API Error (Prob 4): {e}")
        return

    df = pd.DataFrame(data)
    if df.empty:
        print("No relief time series data found.")
        return

    # Process Dates
    df['date'] = pd.to_datetime(df['date'], dayfirst=True, errors='coerce')
    df = df.dropna(subset=['date']).sort_values('date')
    df['date_str'] = df['date'].dt.strftime('%d/%m')

    # Get unique categories (excluding Other)
    categories = [c for c in df['category'].unique() if c != 'Other']

    plt.figure(figsize=(12, 6))

    for cat in categories:
        cat_data = df[df['category'] == cat]
        if not cat_data.empty:
            plt.plot(cat_data['date_str'], cat_data['positive'], marker='o', label=cat)

    plt.title('Positive Relief Sentiment Trends (Problem 4)')
    plt.xlabel('Date')
    plt.ylabel('Positive Mentions')
    plt.legend(title="Relief Item")
    plt.grid(True)
    plt.xticks(rotation=45)
    plt.tight_layout()

    plt.show()

# --- MAIN EXECUTION ---
if __name__ == "__main__":
    # Ensure server is running
    try:
        requests.get(f"{API_BASE_URL}/docs", timeout=2)
    except:
        print(f"❌ Error: Cannot connect to {API_BASE_URL}. Run 'python main.py' first.")
        exit()

    # Run the plots
    # Note: Each function calls plt.show(), which blocks execution until the window is closed.
    plot_problem_2()
    plot_problem_3()
    plot_problem_4()
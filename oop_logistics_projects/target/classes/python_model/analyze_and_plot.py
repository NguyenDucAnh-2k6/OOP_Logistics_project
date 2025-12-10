import csv
import requests
import matplotlib.pyplot as plt
import pandas as pd  # Make sure to install pandas: pip install pandas

# 1. SETUP PATHS
CSV_PATH = "D:\JAVAProjects\OOP_Logistics_project\YagiComments.csv"  # Ensure this matches your file name
API_URL = "http://localhost:8000/sentiment-time-series"

texts = []
dates = []

print(f"Reading data from {CSV_PATH}...")

# 2. READ CSV
# Handling the specific headers from YagiComments.csv: "Thời gian" and "Text"
try:
    with open(CSV_PATH, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            # Map CSV headers to what the API expects
            if "Text" in row and "Thời gian" in row:
                texts.append(row["Text"])
                dates.append(row["Thời gian"])
            elif "text" in row and "date" in row:
                # Fallback to standard headers if you changed them
                texts.append(row["text"])
                dates.append(row["date"])
except FileNotFoundError:
    print(f"Error: Could not find file {CSV_PATH}")
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

# 4. PROCESS & PLOT DATA
# Convert JSON response to Pandas DataFrame for easier plotting
df = pd.DataFrame(data)

# Convert 'date' column to datetime objects for proper sorting
df['date'] = pd.to_datetime(df['date'], dayfirst=True) # dayfirst=True handles 7/9/2024 correctly
df = df.sort_values('date')

# Format date back to string for display on X-axis
df['date_str'] = df['date'].dt.strftime('%d/%m/%Y')

# Plotting
plt.figure(figsize=(10, 6))

# Plot lines for each sentiment
plt.plot(df['date_str'], df['positive'], marker='o', label='Positive', color='green')
plt.plot(df['date_str'], df['negative'], marker='o', label='Negative', color='red')
plt.plot(df['date_str'], df['neutral'], marker='o', label='Neutral', color='gray')

plt.title('Sentiment Trends Over Time (Manual Data)')
plt.xlabel('Date')
plt.ylabel('Number of Comments')
plt.legend()
plt.grid(True)
plt.xticks(rotation=45)
plt.tight_layout()

# Show the plot
plt.show()
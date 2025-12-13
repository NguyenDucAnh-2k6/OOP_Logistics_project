import csv
import requests

CSV_PATH = "D:\JAVAProjects\OOP_Logistics_project\YagiComments.csv"

texts = []
dates = []

# Đọc CSV vào lists
with open(CSV_PATH, "r", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        texts.append(row["text"])
        dates.append(row["date"])

# Payload đúng chuẩn API
payload = {
    "texts": texts,
    "dates": dates
}

# Gửi request sang API Python đang chạy
resp = requests.post("http://localhost:8000/sentiment-time-series", json=payload)

print("\n=== KẾT QUẢ SENTIMENT THEO THỜI GIAN ===")
print(resp.json())

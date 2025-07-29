import random
import requests
import time
from concurrent.futures import ThreadPoolExecutor

URL = "http://localhost:8080/api/admin/login"  # 주소 수정 필요
TOTAL_USERS = 1000
SUCCESS_RATE = 0.9
TOTAL_REQUESTS = int(TOTAL_USERS / SUCCESS_RATE)
DURATION_SECONDS = 100
DELAY = DURATION_SECONDS / TOTAL_REQUESTS

DEVICES = ["WEB", "TABLET", "MOBILE_ANDROID", "MOBILE_IOS"]

def send_request(i):
  user_id = f"user{i % TOTAL_USERS + 1:06d}"
  result = "SUCCESS" if random.random() < SUCCESS_RATE else "FAILURE"
  device = random.choice(DEVICES)
  payload = {
    "userId": user_id,
    "device": device,
    "result": result
  }
  try:
    res = requests.post(URL, json=payload)
    print(f"[{res.status_code}] {payload}")
  except Exception as e:
    print(f"❌ Error: {e}")

if __name__ == "__main__":
  with ThreadPoolExecutor(max_workers=10) as executor:
    for i in range(TOTAL_REQUESTS):
      executor.submit(send_request, i)
      time.sleep(DELAY)
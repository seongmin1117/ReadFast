import pymysql
from faker import Faker
from datetime import datetime, timedelta
import random

conn = pymysql.connect(
    host='localhost',
    port=3310,
    user='root',
    password='1234',
    database='read-fast',
    charset='utf8mb4'
)
cursor = conn.cursor()
faker = Faker()

rows = []
for _ in range(100_000):
    user_id = f"user{random.randint(1, 1000)}"
    created_at = datetime.now() - timedelta(days=random.randint(0, 7))
    device = random.choice(['iPhone 15', 'Galaxy S24', 'MacBook Pro', 'iPad Air', 'Windows 11'])
    endpoint = random.choice(['/auth/login', '/auth/verify', '/auth/register', '/auth/refresh'])
    result = random.choice(['SUCCESS', 'FAIL'])
    rows.append((created_at, device, endpoint, result, user_id))

cursor.executemany("""
    INSERT INTO auth_log_entity (date, device, endpoint, result, user_id)
    VALUES (%s, %s, %s, %s, %s)
""", rows)

conn.commit()
cursor.close()
conn.close()
print("✅ 더미 데이터 삽입 완료!")

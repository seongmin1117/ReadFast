import datetime
import gzip
import os
import random
import sqlite3
import time
from typing import List, Tuple


class GzippedSQLiteGenerator:
  def __init__(self, output_dir="./archive-data"):
    self.output_dir = output_dir
    os.makedirs(self.output_dir, exist_ok=True)

    self.users = [f'user{i:06d}' for i in range(1, 50001)]  # 50K users
    self.endpoints = [
      '/api/login', '/api/logout', '/api/profile', '/api/dashboard',
      '/api/settings', '/api/data', '/api/reports', '/api/admin',
      '/api/notifications', '/api/search', '/api/upload', '/api/download'
    ]
    self.devices = ['WEB', 'MOBILE_ANDROID', 'MOBILE_IOS', 'TABLET']
    self.results = ['SUCCESS', 'FAILURE']
    self.result_weights = [90, 10]

  def generate_records_for_date(self, target_date: datetime.date, count: int = 10000) -> List[Tuple]:
    records = []
    # UTC 기준으로 00:00:00Z는 KST 기준 전날 15:00
    base_utc = datetime.datetime.combine(target_date, datetime.time(hour=15)) - datetime.timedelta(days=1)

    date_prefix = target_date.strftime('%Y%m%d')  # 예: 20240701

    interval = 86400 // count  # 초당 하나씩 고르게 분배

    for i in range(count):
      timestamp = base_utc + datetime.timedelta(seconds=i * interval)
      user_id = random.choice(self.users)
      endpoint = random.choice(self.endpoints)
      device = random.choice(self.devices)
      result = random.choices(self.results, weights=self.result_weights)[0]

      record_id = int(f"{date_prefix}{i + 1:05d}")  # 예: 2024070100001
      records.append((record_id, timestamp.isoformat() + "Z", device, user_id, result, endpoint))

    return records

  def create_sqlite_and_compress(self, date: datetime.date, records: List[Tuple]):
    folder = os.path.join(self.output_dir, date.strftime("%Y/%m"))
    os.makedirs(folder, exist_ok=True)

    sqlite_file = os.path.join(folder, f"{date.strftime('%Y-%m-%d')}.sqlite")
    gzip_file = f"{sqlite_file}.gz"

    conn = sqlite3.connect(sqlite_file)
    cursor = conn.cursor()
    cursor.execute("""
                   CREATE TABLE IF NOT EXISTS auth_log (
                                                           id INTEGER PRIMARY KEY,
                                                           date TEXT NOT NULL,
                                                           device TEXT,
                                                           user_id TEXT,
                                                           result TEXT,
                                                           endpoint TEXT
                   )
                   """)
    cursor.executemany("""
                       INSERT INTO auth_log (id, date, device, user_id, result, endpoint)
                       VALUES (?, ?, ?, ?, ?, ?)
                       """, records)
    conn.commit()
    conn.close()

    with open(sqlite_file, 'rb') as f_in, gzip.open(gzip_file, 'wb') as f_out:
      f_out.writelines(f_in)

    os.remove(sqlite_file)
    return gzip_file

  def generate_date_range(self, start_date: str, end_date: str):
    start = datetime.datetime.strptime(start_date, '%Y-%m-%d').date()
    end = datetime.datetime.strptime(end_date, '%Y-%m-%d').date()
    return [start + datetime.timedelta(days=i) for i in range((end - start).days + 1)]

  def generate_all_data(self, start_date: str = '2024-07-01', end_date: str = '2024-12-31'):
    dates = self.generate_date_range(start_date, end_date)
    start_time = time.time()

    for i, date in enumerate(dates, 1):
      print(f"📅 [{i}/{len(dates)}] {date} - 데이터 생성 중...")
      records = self.generate_records_for_date(date, 10000)
      gzip_path = self.create_sqlite_and_compress(date, records)
      print(f"✅ {gzip_path} 생성 완료 ({len(records)} 레코드)\n")

    total_time = time.time() - start_time
    print(f"🎉 모든 작업 완료! 총 소요 시간: {total_time / 60:.1f}분")


# ✅ 실행
generator = GzippedSQLiteGenerator()
generator.generate_all_data(start_date='2025-01-01', end_date='2025-06-30')

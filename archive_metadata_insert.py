import mysql.connector
from datetime import datetime, timedelta

# ✅ MySQL 연결 설정
conn = mysql.connector.connect(
    host='localhost',
    port=3310,
    user='root',
    password='1234',
    database='read-fast'
)
cursor = conn.cursor()

# ✅ 날짜 범위 설정
start_date = datetime(2024, 7, 1)
end_date = datetime(2025, 6, 30)
delta = timedelta(days=1)

# ✅ 기본 설정 값
storage_type = 'sqlite'
compression_type = 'gzip'
record_count = 10000
file_size_bytes = 1800000  # 예시값 (약 1.8MB)

while start_date <= end_date:
  yyyy_mm = start_date.strftime('%Y/%m')
  yyyy_mm_dd = start_date.strftime('%Y-%m-%d')
  filepath = f"./archive-data/{yyyy_mm}/{yyyy_mm_dd}.sqlite.gz"

  KST_TO_UTC = timedelta(hours=-9)

  start_instant = start_date.replace(hour=0, minute=0, second=0, microsecond=0) + KST_TO_UTC
  end_instant = start_date.replace(hour=23, minute=59, second=59, microsecond=0) + KST_TO_UTC
  archived_at = (start_date + timedelta(days=30)).replace(hour=2, minute=0, second=0, microsecond=0) + KST_TO_UTC

  sql = """
        INSERT INTO archive_metadata
        (archived_at, compression_type, deleted, end_date, file_path,
         file_size_bytes, record_count, start_date, storage_type)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s) \
        """
  params = (
    archived_at,
    compression_type,
    False,
    end_instant,
    filepath,
    file_size_bytes,
    record_count,
    start_instant,
    storage_type
  )

  cursor.execute(sql, params)
  print(f"✅ Inserted metadata for {yyyy_mm_dd}")
  start_date += delta

# ✅ 커밋 및 종료
conn.commit()
cursor.close()
conn.close()
print("🎉 All metadata inserted successfully.")

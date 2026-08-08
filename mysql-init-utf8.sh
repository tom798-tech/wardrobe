#!/usr/bin/env bash
set -e

echo "[mysql-init-utf8.sh] Importing wardrobe-content.sql with --default-character-set=utf8mb4 ..."
echo "  MYSQL_DATABASE=${MYSQL_DATABASE}"
echo "  MYSQL_ROOT_PASSWORD=***"

# 关键：显式指定 --default-character-set=utf8mb4 导入，避免 entrypoint 默认使用 latin1 client 导致中文乱码
mysql --default-character-set=utf8mb4 \
  -uroot \
  -p"${MYSQL_ROOT_PASSWORD}" \
  "${MYSQL_DATABASE}" < /tmp/wardrobe-content.sql

echo "[mysql-init-utf8.sh] Import finished successfully."

# 验证一下字符集
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "
USE ${MYSQL_DATABASE};
SELECT '---- DB/charset verify ----' AS info;
SHOW VARIABLES LIKE 'character_set_database';
SELECT '---- Sample brand rows ----' AS info;
SELECT id, brand_name FROM t_brand LIMIT 3;
SELECT '---- Sample clothes rows ----' AS info;
SELECT id, cloth_name, style FROM t_clothes LIMIT 3;
"

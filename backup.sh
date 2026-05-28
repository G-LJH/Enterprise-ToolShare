#!/bin/bash

# PostgreSQL 数据库备份脚本
# 用法: ./backup.sh

set -e

# 加载环境变量
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# 配置
BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/toolshare_backup_${TIMESTAMP}.sql.gz"

# 创建备份目录
mkdir -p "${BACKUP_DIR}"

echo "========================================="
echo "开始数据库备份"
echo "时间: $(date)"
echo "========================================="

# 执行备份
docker exec toolshare-postgres pg_dump -U "${POSTGRES_USER}" "${POSTGRES_DB}" | gzip > "${BACKUP_FILE}"

if [ $? -eq 0 ]; then
    BACKUP_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
    echo "✓ 备份成功: ${BACKUP_FILE} (${BACKUP_SIZE})"
else
    echo "✗ 备份失败!"
    exit 1
fi

# 清理旧备份
echo ""
echo "清理 ${RETENTION_DAYS} 天前的备份..."
find "${BACKUP_DIR}" -name "toolshare_backup_*.sql.gz" -mtime +${RETENTION_DAYS} -delete
echo "✓ 清理完成"

# 显示备份列表
echo ""
echo "当前备份文件:"
ls -lh "${BACKUP_DIR}/"toolshare_backup_*.sql.gz 2>/dev/null || echo "无备份文件"

echo ""
echo "========================================="
echo "备份完成"
echo "========================================="

#!/bin/bash

# 数据库恢复脚本
# 用法: ./restore.sh <backup_file.sql.gz>

set -e

if [ -z "$1" ]; then
    echo "用法: ./restore.sh <backup_file.sql.gz>"
    echo ""
    echo "可用的备份文件:"
    ls -lh ./backups/toolshare_backup_*.sql.gz 2>/dev/null || echo "无备份文件"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "错误: 备份文件不存在: ${BACKUP_FILE}"
    exit 1
fi

# 加载环境变量
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

echo "========================================="
echo "开始数据库恢复"
echo "备份文件: ${BACKUP_FILE}"
echo "时间: $(date)"
echo "========================================="

# 确认操作
read -p "警告: 这将覆盖当前数据库! 是否继续? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "取消恢复"
    exit 0
fi

# 执行恢复
gunzip -c "${BACKUP_FILE}" | docker exec -i toolshare-postgres psql -U "${POSTGRES_USER}" "${POSTGRES_DB}"

if [ $? -eq 0 ]; then
    echo "✓ 恢复成功!"
else
    echo "✗ 恢复失败!"
    exit 1
fi

echo "========================================="
echo "恢复完成"
echo "========================================="

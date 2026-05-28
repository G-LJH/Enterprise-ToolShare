#!/bin/bash

# 一键部署脚本（生产环境）
# 用法: ./deploy.sh

set -e

echo "========================================="
echo "Tool Share 生产环境部署"
echo "时间: $(date)"
echo "========================================="

# 检查 .env 文件
if [ ! -f .env ]; then
    echo "✗ 错误: .env 文件不存在!"
    echo "请复制 .env.example 并配置:"
    echo "  cp .env.example .env"
    echo "  vim .env"
    exit 1
fi

# 加载环境变量
export $(cat .env | grep -v '^#' | xargs)

echo ""
echo "配置信息:"
echo "  数据库: ${POSTGRES_DB}"
echo "  后端端口: ${BACKEND_PORT}"
echo "  前端端口: ${FRONTEND_PORT}"
echo ""

# 创建必要目录
echo "创建必要目录..."
mkdir -p backups

# 停止旧服务
echo ""
echo "停止旧服务..."
docker compose down || true

# 构建并启动
echo ""
echo "构建并启动服务..."
docker compose up -d --build

# 等待服务启动
echo ""
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "服务状态:"
docker compose ps

# 健康检查
echo ""
echo "执行健康检查..."
sleep 5

BACKEND_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${BACKEND_PORT}/api/health 2>/dev/null || echo "000")
if [ "${BACKEND_HEALTH}" = "200" ]; then
    echo "✓ 后端健康检查通过"
else
    echo "✗ 后端健康检查失败 (HTTP ${BACKEND_HEALTH})"
fi

echo ""
echo "========================================="
echo "部署完成!"
echo "========================================="
echo ""
echo "访问地址:"
echo "  前端:  http://服务器IP:${FRONTEND_PORT}"
echo "  后端:  http://服务器IP:${BACKEND_PORT}/api/health"
echo ""
echo "查看日志:"
echo "  docker compose logs -f"
echo ""
echo "备份数据库:"
echo "  ./backup.sh"
echo "========================================="

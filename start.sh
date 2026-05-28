#!/bin/bash

# Tool Share 快速启动脚本
# 用于 Linux/Mac 环境

set -e

echo "========================================"
echo "  Tool Share 快速启动脚本"
echo "========================================"
echo ""

# 检查 Docker 是否运行
echo "[1/4] 检查 Docker 环境..."
if ! docker version >/dev/null 2>&1; then
    echo "  错误: Docker 未运行或未安装，请先启动 Docker"
    exit 1
fi
DOCKER_VERSION=$(docker version --format '{{.Server.Version}}')
echo "  Docker 版本: $DOCKER_VERSION"
echo ""

# 检查 .env 文件
echo "[2/4] 检查配置文件..."
if [ ! -f ".env" ]; then
    echo "  警告: .env 文件不存在，正在从 .env.example 复制..."
    if [ -f ".env.example" ]; then
        cp .env.example .env
        echo "  已创建 .env 文件，请检查配置是否正确"
    else
        echo "  错误: .env.example 文件也不存在"
        exit 1
    fi
else
    echo "  .env 文件存在"
fi
echo ""

# 启动服务
echo "[3/4] 启动服务..."
echo "  正在启动 PostgreSQL、Backend 和 Frontend..."
docker-compose up -d --build

if [ $? -ne 0 ]; then
    echo "  错误: 服务启动失败"
    exit 1
fi
echo "  服务启动命令已执行，等待服务就绪..."
echo ""

# 等待服务就绪
echo "[4/4] 等待服务就绪..."

echo "  等待 PostgreSQL 启动..."
TIMEOUT=60
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    HEALTHY=$(docker inspect --format='{{.State.Health.Status}}' toolshare-postgres 2>/dev/null)
    if [ "$HEALTHY" = "healthy" ]; then
        echo "  PostgreSQL 已就绪"
        break
    fi
    sleep 2
    ELAPSED=$((ELAPSED + 2))
done

echo "  等待 Backend 启动..."
TIMEOUT=120
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    HEALTHY=$(docker inspect --format='{{.State.Health.Status}}' toolshare-backend 2>/dev/null)
    if [ "$HEALTHY" = "healthy" ]; then
        echo "  Backend 已就绪"
        break
    fi
    sleep 2
    ELAPSED=$((ELAPSED + 2))
done

# 检查 Frontend
sleep 5
if docker ps --filter "name=toolshare-frontend" --format '{{.Status}}' >/dev/null 2>&1; then
    echo "  Frontend 已启动"
fi

echo ""
echo "========================================"
echo "  启动完成！"
echo "========================================"
echo ""
echo "访问地址:"
echo "  前端: http://localhost:3000"
echo "  后端: http://localhost:8080"
echo ""
echo "默认管理员账号:"
echo "  用户名: admin"
echo "  密码: Admin@123456"
echo ""
echo "常用命令:"
echo "  查看日志: docker-compose logs -f"
echo "  停止服务: docker-compose down"
echo "  重启服务: docker-compose restart"
echo ""

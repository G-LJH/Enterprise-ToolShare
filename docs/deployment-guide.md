# 部署运维说明

这份文档面向研发、运维、实施同事，覆盖从本地启动到公司内网生产部署的完整过程。

## 1. 部署目标

项目当前最推荐的部署方式是 Docker Compose。原因很直接：

- 前端、后端、数据库边界清晰
- 初始化成本低
- 便于在公司内网、测试机、云主机或虚拟机上统一落地
- 备份、迁移、升级都更容易标准化

当前仓库提供两套编排文件：

- `docker-compose.yml`：开发 / 测试 / 本机演示
- `docker-compose.prod.yml`：生产 / 内网服务器

## 2. 系统架构

```text
浏览器
  -> Frontend (Next.js, 3000)
      -> Backend (Spring Boot, 8080)
          -> PostgreSQL (5432, 容器内)
```

### 2.1 组件说明

- Frontend：提供页面和交互界面。
- Backend：提供认证、权限、业务逻辑和 API。
- PostgreSQL：保存账号、角色、工具、工作流、审核单、日志等数据。
- Flyway：在后端启动时自动执行数据库迁移。

### 2.2 默认端口

| 组件 | 端口 | 说明 |
| --- | --- | --- |
| Frontend | `3000` | 用户访问地址 |
| Backend | `8080` | API 和健康检查 |
| PostgreSQL | `5432` | 开发环境可暴露；生产不建议默认暴露 |

## 3. 服务器建议

### 3.1 最低建议

- CPU：2 核
- 内存：4 GB
- 磁盘：40 GB
- 操作系统：Linux 64 位

### 3.2 更稳妥的生产建议

- CPU：4 核
- 内存：8 GB
- 磁盘：80 GB 以上 SSD
- 具备 Docker / Docker Compose 环境

## 4. 前置准备

### 4.1 软件要求

- Docker
- Docker Compose

如果需要本机开发，还需要：

- Java 21
- Maven 3.9+
- Node.js 20+

### 4.2 仓库内关键文件

- `.env.example`
- `.env.prod.example`
- `docker-compose.yml`
- `docker-compose.prod.yml`
- `start.ps1`
- `start.sh`
- `deploy.sh`
- `backup.sh`
- `restore.sh`

说明：

- `start.ps1` / `start.sh` 更适合本地快速拉起。
- `deploy.sh` 是一个简化部署脚本，默认读取 `.env` 并调用默认编排。
- 公司正式环境更建议直接使用 `.env.prod` + `docker-compose.prod.yml` 的方式部署，流程更可控。

## 5. 环境变量说明

### 5.1 开发环境模板

开发环境可从 `.env.example` 复制：

```bash
cp .env.example .env
```

### 5.2 生产环境模板

生产环境可从 `.env.prod.example` 复制：

```bash
cp .env.prod.example .env.prod
```

### 5.3 重点变量

| 变量名 | 说明 |
| --- | --- |
| `POSTGRES_DB` | 数据库名 |
| `POSTGRES_USER` | 数据库用户名 |
| `POSTGRES_PASSWORD` | PostgreSQL 初始化密码 |
| `SPRING_DATASOURCE_URL` | 后端数据库连接串 |
| `SPRING_DATASOURCE_USERNAME` | 后端数据库账号 |
| `SPRING_DATASOURCE_PASSWORD` | 后端数据库密码 |
| `BACKEND_PORT` | 宿主机映射的后端端口 |
| `FRONTEND_PORT` | 宿主机映射的前端端口 |
| `NEXT_PUBLIC_BACKEND_BASE_URL` | 前端调用后端 API 的地址 |
| `CORS_ALLOWED_ORIGINS` | 后端允许访问的前端来源 |
| `LOGGING_FILE_NAME` | 后端日志文件路径 |
| `BACKUP_DIR` | 备份输出目录 |
| `BACKUP_RETENTION_DAYS` | 备份保留天数 |

### 5.4 两个容易忽略的点

#### `POSTGRES_PASSWORD` 与 `SPRING_DATASOURCE_PASSWORD` 要一致

数据库容器初始化时使用 `POSTGRES_PASSWORD`，后端连接时使用 `SPRING_DATASOURCE_PASSWORD`。两者不一致会导致后端无法连库。

#### `NEXT_PUBLIC_BACKEND_BASE_URL` 变更后要重建前端

前端镜像在构建阶段会注入这个变量。如果生产环境后端地址变化，建议重新构建并发布前端镜像，而不是只改运行时变量。

## 6. 本地开发启动

### 6.1 方式一：全部走 Docker

```bash
docker-compose up -d --build
```

访问：

- `http://localhost:3000`
- `http://localhost:8080/api/health`

### 6.2 方式二：数据库用 Docker，前后端本机跑

先启动数据库：

```bash
docker-compose up -d postgres
```

启动后端：

```bash
cd backend
mvn spring-boot:run
```

如果后端在宿主机运行，数据库地址要指向 `localhost`，例如：

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tool_share
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=你的密码
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
```

启动前端：

```bash
cd frontend
npm install
npm run dev
```

### 6.3 使用快速脚本

- Windows PowerShell：`./start.ps1`
- Linux / macOS：`./start.sh`
- Windows CMD：`start.bat`

这些脚本会自动检查 `.env`、启动容器并等待服务就绪。

## 7. 标准生产部署

### 7.1 适用场景

适合以下情况：

- 公司内网服务器
- 测试环境
- 不依赖 Kubernetes 的中小规模部署
- 需要快速交付给业务团队试运行
- 没有使用 1Panel、宝塔、Kubernetes 等管理平台的标准 Linux 服务器

### 7.2 推荐目录准备

将项目放在固定业务目录下，例如：

```text
/opt/tool-share
```

生产部署后，建议关注这些目录：

- `./data/postgres`：数据库数据
- `./logs/backend`：后端日志
- `./backups`：备份文件

### 7.3 部署步骤

#### 第一步：准备生产配置

```bash
cp .env.prod.example .env.prod
```

至少修改：

- `POSTGRES_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `NEXT_PUBLIC_BACKEND_BASE_URL`
- `CORS_ALLOWED_ORIGINS`

通用填写建议：

- `NEXT_PUBLIC_BACKEND_BASE_URL` 填后端实际访问地址，例如 `http://your-server-ip:8080`
- `CORS_ALLOWED_ORIGINS` 填前端实际访问地址，例如 `http://your-server-ip:3000`
- 如果公司有域名，也可以改成 `https://tools.company.com` 和对应后端域名

#### 第二步：启动生产服务

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

#### 第三步：检查容器状态

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
```

#### 第四步：执行健康检查

```bash
curl http://服务器IP:8080/api/health
```

#### 第五步：访问前端

```text
http://服务器IP:3000
```

如果公司通过反向代理或域名访问，也可以是：

```text
https://你的业务域名
```

### 7.4 首次初始化检查清单

首次部署完成后建议按下面顺序检查：

1. 前端页面能否打开
2. 后端健康检查是否返回成功
3. 能否用默认管理员 `admin / Admin@123456` 登录
4. 是否能看到工具目录页面
5. 能否打开“账号权限”“审核中心”等管理页面
6. 查看数据库迁移是否已完成
7. 检查后端日志中是否有启动报错

## 8. 通用公司内网部署建议

大多数公司在内网部署这类系统时，通常会落在下面三种方式之一：

### 8.1 方式一：直接用 IP + 端口访问

这是最简单的方式，适合内部试运行或小范围使用：

- 前端：`http://服务器IP:3000`
- 后端：`http://服务器IP:8080`

适用场景：

- 内网测试
- 部门级使用
- 暂时没有统一域名或网关接入

### 8.2 方式二：通过公司反向代理或网关访问

如果公司已有 Nginx、Traefik、Ingress、F5、网关平台等，推荐让前端和后端都走统一访问入口。

常见做法：

- 对外暴露一个前端域名，例如 `https://tools.company.com`
- 由前端去访问后端域名或网关路径
- 后端只对内网开放，不直接暴露到更大范围

这种方式的优点是：

- 地址更稳定
- 更容易接入公司统一证书
- 更容易做访问控制和流量治理

### 8.3 方式三：通过容器管理平台部署

如果公司使用的是 1Panel、宝塔、Portainer、Rancher 等平台，也可以继续使用本项目提供的 Docker 编排文件接入这些平台。

原则上只要平台支持 Docker Compose 或等价的多容器编排，就可以部署本项目。

### 8.4 平台无关的部署建议

无论公司使用哪种平台，都建议遵守下面这些原则：

- 前端和后端地址提前规划，不要频繁更改
- 数据库数据目录单独挂载，避免容器重建丢数据
- 日志目录单独挂载，方便排障和归档
- 数据库端口默认不对外暴露
- 升级前先备份数据库
- 首次上线后立即修改默认管理员密码

### 8.5 1Panel 说明

如果某些团队正好使用 1Panel，可额外参考：

- [1Panel 内网直连部署说明](1panel-intranet-deploy.md)

但这不是本项目的默认前提，也不是唯一推荐方案。

## 9. 默认账号与安全建议

### 9.1 默认管理员

系统启动时会自动创建：

- 用户名：`admin`
- 密码：`Admin@123456`

### 9.2 上线后必须执行

1. 首次登录后立即修改管理员密码
2. 不要在仓库里保留真实生产密码
3. 限制服务器防火墙，只开放前端和后端业务端口
4. 生产环境不要默认暴露数据库端口
5. 建立备份计划后再开放给更多用户使用

### 9.3 当前内置安全基线

系统当前包含这些基础安全控制：

- 基于账号和角色的访问控制
- 登录失败次数限制
- 登录锁定时间限制
- 会话过期时间控制
- 管理操作审计日志

默认配置里：

- 会话有效期：12 小时
- 连续失败上限：5 次
- 锁定时长：15 分钟

## 10. 备份与恢复

### 10.1 执行备份

开发 / 默认编排可以直接执行：

```bash
./backup.sh
```

脚本会：

- 连接 PostgreSQL 容器
- 导出 SQL
- 压缩保存到 `BACKUP_DIR`
- 清理超过保留天数的旧备份

注意：

- `backup.sh` 默认连接容器名 `toolshare-postgres`
- 如果你使用的是 `docker-compose.prod.yml`，数据库容器名通常是 `toolshare-postgres-prod`
- 生产环境建议执行等价命令，或先把脚本中的容器名调整为生产容器名

生产环境可参考：

```bash
docker exec toolshare-postgres-prod pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" | gzip > ./backups/toolshare_backup_$(date +%Y%m%d_%H%M%S).sql.gz
```

### 10.2 恢复备份

开发 / 默认编排可以直接执行：

```bash
./restore.sh ./backups/你的备份文件.sql.gz
```

注意：

- 恢复会覆盖当前数据库
- 恢复前务必再次确认目标环境

如果生产环境使用的是 `toolshare-postgres-prod`，可执行等价命令：

```bash
gunzip -c ./backups/你的备份文件.sql.gz | docker exec -i toolshare-postgres-prod psql -U "$POSTGRES_USER" "$POSTGRES_DB"
```

### 10.3 生产建议

- 至少每日一次数据库备份
- 版本升级前强制做一次手工备份
- 备份目录建议同步到独立存储或备份盘

## 11. 日志与问题排查

### 11.1 查看容器日志

开发编排：

```bash
docker compose logs -f
```

生产编排：

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f
```

### 11.2 查看后端健康状态

```bash
curl http://服务器IP:8080/api/health
```

### 11.3 常见问题

#### 问题 1：后端启动失败，提示数据库连接失败

优先检查：

- PostgreSQL 容器是否健康
- `POSTGRES_PASSWORD` 与 `SPRING_DATASOURCE_PASSWORD` 是否一致
- `SPRING_DATASOURCE_URL` 是否指向正确主机名

#### 问题 2：前端页面打开了，但接口请求失败

优先检查：

- `NEXT_PUBLIC_BACKEND_BASE_URL` 是否正确
- `CORS_ALLOWED_ORIGINS` 是否包含前端访问地址
- 修改前端地址后是否重新构建了前端镜像

#### 问题 3：升级后数据库异常

优先检查：

- Flyway 迁移是否执行成功
- 升级前是否已备份
- 后端启动日志里是否有 migration 相关错误

## 12. 升级建议流程

建议按下面顺序升级：

1. 通知业务低峰期维护窗口
2. 执行数据库备份
3. 拉取最新代码
4. 检查 `.env.prod` 是否需要补充新变量
5. 执行重新构建与重启
6. 检查健康状态
7. 登录管理员账号做关键路径验证

示例命令：

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml down
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

关键验证建议至少覆盖：

- 登录
- 工具目录浏览
- 审核中心打开
- 账号管理打开
- 导入导出页打开

## 13. 推荐交付方式

如果这个系统要交付给公司内部使用，建议最终交付物至少包含：

1. 本仓库代码
2. `.env.prod` 配置样例
3. 本文档
4. 1Panel 部署补充说明
5. 初始管理员账号交接说明
6. 备份与升级操作清单

这样后续无论是研发维护、实施落地还是业务培训，都更容易接得住。

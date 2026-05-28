# 企具共享 (Enterprise ToolShare)

> 企业内部工具共享平台，让团队高效复用优质工具资产

企业级工具共享与管理平台，提供工具发布、工作流编排、权限控制、协作评价等核心能力，助力企业沉淀和共享工具资产，提升团队协同效率。

## 技术栈

- **前端**: Next.js 14 + TypeScript + Ant Design
- **后端**: Spring Boot 3.3.2 + Java 21
- **数据库**: PostgreSQL 16
- **部署**: Docker Compose

## 快速开始

### 前置要求

- Docker & Docker Compose
- Node.js 20+（仅本机开发需要）
- Java 21 + Maven（仅本机开发需要）

### 方式一：Docker Compose（推荐）

```bash
docker-compose up -d
```

启动后访问：
- 前端：http://localhost:3000
- 后端：http://localhost:8080
- 数据库：localhost:5432

### 方式二：混合模式（数据库在 Docker，前后端本机运行）

1. 启动数据库：
```bash
docker-compose up -d postgres
```

2. 启动后端（需要 Java 21）：
```bash
cd backend
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/tool_share"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="ToolShare@2026!Secure"
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.postgresql.Driver"
mvn spring-boot:run
```

3. 启动前端（需要 Node.js 20+）：
```bash
cd frontend
npm install
npm run dev
```

## 默认账号

### 管理员账号

系统启动时会自动创建默认管理员账号：

- **用户名**: `admin`
- **密码**: `Admin@123456`

### 数据库

- **用户名**: `postgres`
- **密码**: `ToolShare@2026!Secure`

## 项目结构

```
tool-share/
├── backend/          # Spring Boot 后端
├── frontend/         # Next.js 前端
├── docker-compose.yml
└── .env              # 环境变量
```

## API 文档

### 健康检查

```bash
GET http://localhost:8080/api/health
```

### 数据库版本

```bash
GET http://localhost:8080/api/db/version
```

## 环境变量

所有环境变量配置在 `.env` 文件中，详见 [.env.example](.env.example)。

## 数据库迁移

项目使用 Flyway 进行数据库版本管理，迁移脚本位于 `backend/src/main/resources/db/migration/`。

## 开发指南

### 后端开发

```bash
cd backend
mvn spring-boot:run
```

运行测试：
```bash
mvn test
```

### 前端开发

```bash
cd frontend
npm run dev
```

构建生产版本：
```bash
npm run build
npm start
```

## 常见问题

### Flyway 报错

如果遇到 `Unsupported Database: PostgreSQL` 错误，确保 `backend/pom.xml` 中包含 `flyway-database-postgresql` 依赖。

### 端口冲突

如果端口被占用，修改 `.env` 文件中的端口配置：
- `BACKEND_PORT=8080`
- `FRONTEND_PORT=3000`

## License

MIT

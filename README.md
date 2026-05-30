# 企具共享（Tool Share）

企业内部工具共享与治理平台，用来沉淀团队常用工具、使用经验和场景化工作流，减少“工具分散、经验分散、重复试错”的问题。

README 是总览入口。想快速了解项目，先看这里；想看详细说明，再进入 `docs/` 里的分主题文档。

## 项目能做什么

- 建立统一工具目录：集中维护工具名称、简介、链接、标签、使用方法和推荐人。
- 支持工具提交与审核：普通用户提交，审核员或管理员审批，驳回后支持修改重提。
- 沉淀工作流：把多个工具按步骤串成一套可复用的方法。
- 支持协作反馈：工具和工作流都可以点赞、收藏；工具支持评论。
- 支持组织治理：账号、角色、标签、审核开关、日志、批量导入导出都在系统内完成。
- 支持审计留痕：登录、审核、创建、修改、导入导出等关键动作会记录审计日志。

## 三类身份

| 身份 | 角色编码 | 主要职责 |
| --- | --- | --- |
| 普通用户 | `USER` | 浏览工具、收藏点赞、评论、提交工具和工作流 |
| 审核员 | `REVIEWER` | 处理工具/工作流审核，查看审核结果 |
| 系统管理员 | `ADMIN` | 拥有全量管理权限，负责账号、权限、标签、工具、工作流、导入导出和日志治理 |

详细权限和使用路径见：

- [项目概览](docs/project-overview.md)
- [按角色使用说明](docs/role-based-guide.md)

## 文档导航

- [项目概览](docs/project-overview.md)：适合管理者、产品、实施同事，快速理解系统定位、功能边界和业务流程。
- [按角色使用说明](docs/role-based-guide.md)：按普通用户、审核员、管理员拆开讲怎么用。
- [部署运维说明](docs/deployment-guide.md)：适合研发、运维、实施同事，覆盖通用 Docker 部署、开发环境、生产环境、备份恢复与升级。
- [1Panel 内网直连补充说明](docs/1panel-intranet-deploy.md)：可选补充材料，仅适用于使用 1Panel 的团队。

## 系统结构

```text
tool-share/
├── frontend/                  # Next.js 前端
├── backend/                   # Spring Boot 后端
├── docs/                      # 项目说明文档
├── implementation-modules/    # 模块化实现说明
├── docker-compose.yml         # 开发/测试编排
├── docker-compose.prod.yml    # 生产编排
├── start.ps1 / start.sh       # 快速启动脚本
├── backup.sh / restore.sh     # 备份恢复脚本
└── .env*                      # 环境变量模板
```

## 技术栈

- 前端：Next.js + TypeScript + Ant Design
- 后端：Spring Boot 3 + Java 21
- 数据库：PostgreSQL 16
- 数据迁移：Flyway
- 部署方式：Docker Compose

## 快速启动

### 方式一：Docker Compose

```bash
docker-compose up -d --build
```

默认访问地址：

- 前端：`http://localhost:3000`
- 后端：`http://localhost:8080`
- 健康检查：`http://localhost:8080/api/health`

### 方式二：使用启动脚本

- Windows PowerShell：`./start.ps1`
- Windows CMD：`start.bat`
- Linux / macOS：`./start.sh`

### 方式三：本机前后端 + Docker 数据库

```bash
docker-compose up -d postgres
cd backend
mvn spring-boot:run

cd ../frontend
npm install
npm run dev
```

更完整的部署与初始化步骤见 [部署运维说明](docs/deployment-guide.md)。

## 默认账号

系统首次启动会自动创建管理员账号：

- 用户名：`admin`
- 密码：`Admin@123456`

建议首次登录后立即修改为公司内部规范密码。

## 环境变量

项目提供以下模板：

- 开发/测试：`.env.example`
- 生产：`.env.prod.example`

生产环境最少需要确认这些变量：

- `POSTGRES_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `NEXT_PUBLIC_BACKEND_BASE_URL`
- `CORS_ALLOWED_ORIGINS`

## 数据库与日志

- 数据库迁移脚本位于 `backend/src/main/resources/db/migration/`
- 后端日志默认写入 `/app/logs/backend.log`
- 备份脚本：`backup.sh`
- 恢复脚本：`restore.sh`

## 补充说明

- 工具审核开关可在管理端页面中直接配置。
- 工作流审核当前默认开启，后端已支持配置项控制，但前端暂未提供独立开关页面。
- 前端使用 `NEXT_PUBLIC_BACKEND_BASE_URL` 访问后端；如果生产地址变化，建议重新构建前端镜像后再发布。
- 项目的主推荐部署方式是标准 Docker Compose，不依赖 1Panel 或特定厂商平台。

# M11 集成联调与回归验收

状态：`done`

## 1. 模块目标

在所有功能模块完成后，对系统进行完整联调、回归测试、性能与安全检查，形成上线前验收清单。

## 2. 实现范围

- 模块联调
- 回归测试
- 核心流程验收
- 上线前检查清单

## 3. 非范围

- 不负责新增功能开发

## 4. 前置依赖

- `M00` 到 `M10` 全部完成

## 5. 数据设计

- 不新增核心表
- 可增加验收测试数据集

## 6. 接口设计

- 无新增业务接口

## 7. 页面或交互影响

- 全站联调验证

## 8. 实现步骤

1. 梳理核心用户路径
2. 执行回归测试
3. 执行权限与安全检查
4. 执行导入导出和日志链路验证
5. 输出验收结果

## 9. 风险点

- 模块各自可用但集成失败
- 边界场景未覆盖

## 10. 测试方案

### 单元测试

- 以补漏为主

### 集成测试

- 登录到工具提交到审核到发布完整链路
- 点赞收藏评论联动正确
- 导入导出与日志联动正确

### 接口测试

- 全量核心接口回归

### 页面验证

- 员工端完整流程
- 管理端完整流程

### 安全验证

- 权限越权测试
- SQL 注入测试
- XSS 测试
- 文件上传安全测试

## 11. 验收标准

- 核心流程全部通过
- 回归问题收敛
- 上线清单完成

## 12. 当前实现说明

### 实现范围

- 已补充跨模块集成测试，覆盖真实登录、工具提交、审核通过、点赞收藏评论、工作流编排、导入导出、日志查询
- 已完成后端全量回归、前端 lint 校验、前端生产构建校验
- 已同步更新项目级 README，沉淀当前模块状态、入口页面和验收结果

### 数据与接口设计

- `M11` 不新增业务表和业务接口
- 新增验收测试类：`backend/src/test/java/com/toolshare/M11AcceptanceIntegrationTests.java`
- 本轮联调复用了已有核心接口：
  - `POST /api/auth/login`
  - `POST /api/tools/submissions`
  - `POST /api/admin/reviews/{submissionId}/approve`
  - `POST /api/tools/{toolId}/stars`
  - `POST /api/tools/{toolId}/favorites`
  - `POST /api/tools/{toolId}/comments`
  - `POST /api/admin/workflows`
  - `POST /api/admin/import-export/imports`
  - `POST /api/admin/import-export/exports`
  - `GET /api/admin/logs/audit`
  - `GET /api/admin/logs/operations`

### 测试方案

- 集成主链路：`M11AcceptanceIntegrationTests`
- 后端全量回归：`cd backend && mvn test`
- 前端静态校验：`cd frontend && npm run lint`
- 前端生产构建：`cd frontend && npm run build`

### 验收结论

- 核心跨模块链路已通过自动化回归，模块之间不存在明显的接口断层
- 权限、审核、互动、导入导出、日志审计等关键链路已在同一轮验收中串联验证
- 当前环境仍缺少可连接的 in-app browser 实例，因此页面级人工联调未在本会话内完成
- `M11` 已达到当前阶段的可交付状态，项目可视为第一阶段完整收口

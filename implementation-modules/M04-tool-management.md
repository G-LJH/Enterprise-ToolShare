# M04 工具管理

状态：`done`

## 1. 模块目标

实现工具的新增、编辑、查看、下架、提交和基础列表能力。

## 2. 实现范围

- 工具新增
- 工具编辑
- 工具详情
- 工具列表
- 工具状态流转
- 用户提交工具

## 3. 非范围

- 不实现复杂搜索
- 不实现审核开关逻辑收口

## 4. 前置依赖

- `M01`
- `M02`
- `M03`

## 5. 数据设计

涉及表：

- `tools`
- `tool_submissions`

需要字段：

- 名称
- 描述
- 链接
- 使用方法
- 推荐人
- 状态

## 6. 接口设计

- 新增工具
- 编辑工具
- 查看工具详情
- 查看工具列表
- 提交工具
- 下架工具

## 7. 页面或交互影响

- 工具列表页
- 工具详情页
- 提交工具页

## 8. 实现步骤

1. 实现工具实体和 DTO
2. 实现工具 CRUD 接口
3. 实现提交工具逻辑
4. 实现工具页面
5. 实现状态展示

## 9. 风险点

- 工具重复提交
- 状态字段流转混乱

## 10. 测试方案

### 单元测试

- 工具状态流转规则
- 工具提交校验逻辑

### 集成测试

- 工具新增编辑写库正确
- 不同状态查询结果正确

### 接口测试

- 用户可提交工具
- 管理员可新增和下架工具

### 页面验证

- 列表、详情、提交页流程正常

### 安全验证

- 非法链接被拦截
- 非授权用户不能执行管理员动作

## 11. 验收标准

- 工具基础管理可用
- 工具提交可用
- 状态正确展示

## 12. 当前实现说明

### 实现范围

- 已实现工具新增、编辑、详情、列表、下架接口
- 已实现普通用户提交工具能力，并生成对应 `tool_submissions`
- 已实现工具状态流转首版规则：`APPROVED`、`PENDING_REVIEW`、`OFFLINE`
- 已实现工具列表页、详情页、提交页、管理员工具管理页

### 数据与接口设计

- 数据库迁移文件为 `backend/src/main/resources/db/migration/V4__m04_tool_management.sql`
- `tools` 表已补充 `url`、`usage_guide`、互动计数字段，并将推荐人字段统一为 `recommender_id`
- 用户侧接口为 `/api/tools`、`/api/tools/{toolId}`、`/api/tools/submissions`
- 管理侧接口为 `/api/admin/tools`、`/api/admin/tools/{toolId}`、`/api/admin/tools/{toolId}/offline`
- 工具模块遵循 `Controller -> Service -> Repository` 分层，所有查询与写入均使用参数化 SQL

### 测试方案

- 单元测试：`ToolStatusPolicyTests`
- 集成/接口测试：`ToolControllerTests`
- 后端全量验证：`cd backend && mvn test`
- 前端校验：`cd frontend && npm run lint && npm run build`

### 验收结论

- 管理员可新增、编辑、下架工具，并写入 `audit_logs`
- 普通用户可提交工具，提交后进入 `PENDING_REVIEW`
- 用户可浏览已发布工具，并查看自己的提交记录
- 工具页面与管理页面已完成基础闭环，可继续作为 `M05`、`M06`、`M07` 的依赖基础

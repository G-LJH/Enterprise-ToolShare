# M08 工具工作流

状态：`done`

## 1. 模块目标

实现“工具工作流”模块，用于沉淀经过验证的高效工具组合方案。

## 2. 实现范围

- 工作流新增
- 工作流编辑
- 工作流列表
- 工作流详情
- 工作流与工具关联

## 3. 非范围

- 不做自动推荐

## 4. 前置依赖

- `M04`
- `M05`

## 5. 数据设计

涉及表：

- `workflows`
- `workflow_tools`

字段建议：

- 名称
- 场景
- 描述
- 步骤说明
- 是否精选

## 6. 接口设计

- 新增工作流
- 编辑工作流
- 工作流列表
- 工作流详情

## 7. 页面或交互影响

- 工具工作流页
- 工作流详情页
- 管理端工作流管理页

## 8. 实现步骤

1. 建立工作流实体和工具关联
2. 实现工作流 CRUD
3. 实现前端列表和详情
4. 增加精选展示

## 9. 风险点

- 步骤与工具关联表达不清
- 工作流内容变成长文本堆积

## 10. 测试方案

### 单元测试

- 工作流步骤数据校验

### 集成测试

- 工作流与工具关联正确

### 接口测试

- 工作流增删改查可用

### 页面验证

- 工作流列表和详情展示正常

### 安全验证

- 非管理员不能编辑精选配置

## 11. 验收标准

- 工作流模块可独立使用
- 工具关联正常
- 页面可浏览与管理

## 12. 当前实现说明

### 实现范围

- 已实现工作流新增、编辑、删除、列表、详情
- 已实现工作流与多个工具的有序关联
- 已实现精选工作流配置和公共侧精选筛选
- 已实现公共工作流列表页、详情页和管理端工作流管理页

### 数据与接口设计

- 数据库迁移文件为 `backend/src/main/resources/db/migration/V7__m08_workflows.sql`
- `workflows` 表新增了 `scenario`、`steps`、`featured`、`creator_id`
- 用户侧接口为：
  - `GET /api/workflows`
  - `GET /api/workflows/{workflowId}`
- 管理侧接口为：
  - `GET /api/admin/workflows`
  - `GET /api/admin/workflows/{workflowId}`
  - `POST /api/admin/workflows`
  - `PUT /api/admin/workflows/{workflowId}`
  - `DELETE /api/admin/workflows/{workflowId}`

### 测试方案

- 单元测试：工作流输入校验已在服务层覆盖
- 集成/接口测试：`WorkflowControllerTests`
- 后端全量验证：`cd backend && mvn test`
- 前端校验：`cd frontend && npm run lint && npm run build`

### 验收结论

- 工作流模块已可独立浏览和管理
- 工作流中的工具关联顺序可正确保存和回显
- 精选配置可由管理员维护，普通用户只读浏览
- `M08` 已形成可验证闭环，可继续作为 `M09`、`M11` 的展示与联调基础

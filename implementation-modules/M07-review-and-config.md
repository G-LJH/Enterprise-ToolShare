# M07 审核流程与系统配置

状态：`done`

## 1. 模块目标

实现工具审核流程和系统审核开关配置。

## 2. 实现范围

- 审核开关配置
- 待审核列表
- 审核通过
- 审核驳回
- 驳回原因记录

## 3. 非范围

- 不实现复杂审批流

## 4. 前置依赖

- `M02`
- `M03`
- `M04`

## 5. 数据设计

涉及表：

- `tool_submissions`
- `system_configs`

状态建议：

- `pending_review`
- `approved`
- `rejected`

## 6. 接口设计

- 查询审核开关
- 修改审核开关
- 查询待审核列表
- 审核通过
- 审核驳回

## 7. 页面或交互影响

- 管理端审核页
- 系统配置页

## 8. 实现步骤

1. 实现配置表读写能力
2. 实现审核状态流转
3. 实现审核接口
4. 实现管理端审核页

## 9. 风险点

- 开关关闭时提交逻辑未同步变化
- 审核后工具正式表与提交表状态不一致

## 10. 测试方案

### 单元测试

- 审核状态流转测试
- 开关逻辑测试

### 集成测试

- 开关开启与关闭两种模式都能正确提交
- 审核通过后工具可见

### 接口测试

- 管理员可审核
- 普通用户不可审核

### 页面验证

- 待审核列表展示正确
- 审核动作反馈正确

### 安全验证

- 非管理员不能修改系统配置

## 11. 验收标准

- 审核链路完整
- 配置开关可用
- 审核权限正确

## 12. 当前实现说明

### 实现范围

- 已实现工具审核开关配置，管理员可开启或关闭“提交后需人工审核”
- 已实现待审核、已通过、已驳回三类审核列表查询
- 已实现审核通过与审核驳回动作，驳回原因可记录到提交单
- 已实现管理端审核页与系统配置页
- 已实现审核开关关闭时普通用户提交自动通过，但仍保留提交记录

### 数据与接口设计

- 数据库迁移文件为 `backend/src/main/resources/db/migration/V6__m07_review_and_config.sql`
- 系统配置键采用 `TOOL_REVIEW_ENABLED`
- 管理侧接口为：
  - `GET /api/admin/system-configs/tool-review`
  - `PUT /api/admin/system-configs/tool-review`
  - `GET /api/admin/reviews`
  - `POST /api/admin/reviews/{submissionId}/approve`
  - `POST /api/admin/reviews/{submissionId}/reject`
- 普通用户提交接口仍为 `/api/tools/submissions`，但提交后的工具状态与提交单状态改为由审核开关决定

### 测试方案

- 单元测试：`ToolStatusPolicyTests`
- 集成/接口测试：`ToolReviewAdminControllerTests`、`ToolControllerTests`
- 后端全量验证：`cd backend && mvn test`
- 前端校验：`cd frontend && npm run lint && npm run build`

### 验收结论

- 审核链路已完整，管理员可在审核台处理普通用户提交
- 审核开关可用，关闭后会自动发布新提交工具
- 工具状态与提交单状态可同步更新，驳回原因可追溯
- 管理接口已受管理员权限保护，关键配置与审核动作会写入 `audit_logs`

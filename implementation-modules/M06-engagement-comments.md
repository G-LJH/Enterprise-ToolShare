# M06 点赞收藏评论

状态：`done`

## 1. 模块目标

实现点赞、收藏、评论三类用户互动能力。

## 2. 实现范围

- 点赞
- 取消点赞
- 收藏
- 取消收藏
- 评论新增
- 评论删除
- 评论列表

## 3. 非范围

- 不做评论楼中楼
- 不做消息提醒

## 4. 前置依赖

- `M03`
- `M04`

## 5. 数据设计

涉及表：

- `tool_stars`
- `tool_favorites`
- `tool_comments`

约束建议：

- 点赞唯一约束 `(user_id, tool_id)`
- 收藏唯一约束 `(user_id, tool_id)`

## 6. 接口设计

- 点赞/取消点赞
- 收藏/取消收藏
- 评论新增
- 评论删除
- 评论列表

## 7. 页面或交互影响

- 工具详情页互动区
- 我的收藏页

## 8. 实现步骤

1. 实现点赞收藏评论表逻辑
2. 实现互动接口
3. 实现冗余计数字段更新
4. 实现前端互动区

## 9. 风险点

- 重复点赞
- 并发下计数不一致
- 评论内容安全问题

## 10. 测试方案

### 单元测试

- 重复点赞保护
- 评论内容校验

### 集成测试

- 点赞和收藏计数更新正确
- 评论写入与删除正确

### 接口测试

- 点赞取消点赞行为正确
- 收藏取消收藏行为正确
- 评论增删正确

### 页面验证

- 互动按钮状态切换正确
- 收藏页能展示已收藏工具

### 安全验证

- 评论做 XSS 过滤
- 非本人或管理员删除评论被拒绝

## 11. 验收标准

- 点赞收藏评论可用
- 计数准确
- 权限正确

## 12. 当前实现说明

### 实现范围

- 已实现点赞、取消点赞、收藏、取消收藏、评论新增、评论删除、评论列表
- 已实现工具详情页互动区，支持点赞、收藏、评论发布与删除
- 已实现“我的收藏”页面，用户可查看自己收藏的工具列表
- 评论删除权限已限制为评论作者本人或管理员

### 数据与接口设计

- 数据库迁移文件为 `backend/src/main/resources/db/migration/V5__m05_tags_and_search.sql`
- `tool_stars`、`tool_favorites`、`tool_comments` 表已在前序迁移中落库，并在 `M06` 中接入业务逻辑
- 用户侧互动接口为：
  - `POST /api/tools/{toolId}/stars`
  - `DELETE /api/tools/{toolId}/stars`
  - `POST /api/tools/{toolId}/favorites`
  - `DELETE /api/tools/{toolId}/favorites`
  - `GET /api/tools/{toolId}/comments`
  - `POST /api/tools/{toolId}/comments`
  - `DELETE /api/tools/{toolId}/comments/{commentId}`
  - `GET /api/tools/favorites`
- 评论内容在服务端经过 HTML 转义，避免脚本直接落库和回显

### 测试方案

- 单元测试：`CommentSanitizerTests`
- 集成/接口测试：`ToolEngagementControllerTests`
- 后端全量验证：`cd backend && mvn test`
- 前端校验：`cd frontend && npm run lint && npm run build`

### 验收结论

- 点赞、收藏、评论能力可用，详情页状态可正确回显
- 重复点赞与重复收藏已被拦截，互动计数字段会同步更新
- 评论删除权限正确，管理员代删会写入审计日志
- `M06` 已形成可验证闭环，可继续作为 `M07` 的用户互动基础

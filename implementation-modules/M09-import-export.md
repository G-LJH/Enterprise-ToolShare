# M09 批量导入导出

状态：`done`

## 1. 模块目标

实现工具数据的批量导入和导出，支持字段选择、模板下载、预校验、任务记录。

## 2. 实现范围

- 导入模板下载
- CSV 导入
- 导入预校验
- 导出字段选择
- CSV 导出
- 导入导出任务记录

## 3. 非范围

- 不处理超大数据量分布式任务

## 4. 前置依赖

- `M04`
- `M05`
- `M02`
- `M03`

## 5. 数据设计

涉及表：

- `import_tasks`
- `export_tasks`

需要记录：

- 发起人
- 文件名
- 状态
- 成功数
- 失败数
- 错误报告路径

## 6. 接口设计

- 下载模板
- 上传导入文件
- 查询导入任务
- 创建导出任务
- 查询导出任务

## 7. 页面或交互影响

- 管理端导入导出管理页

## 8. 实现步骤

1. 设计模板和字段映射
2. 实现导入预校验
3. 实现导入落库
4. 实现导出字段选择
5. 实现任务列表页

## 9. 风险点

- 导入脏数据污染正式表
- 大批量导出阻塞接口

## 10. 测试方案

### 单元测试

- 字段映射逻辑
- 校验规则逻辑

### 集成测试

- 导入成功和失败场景
- 导出结果文件正确

### 接口测试

- 模板下载成功
- 导入返回错误报告
- 导出可按字段生成文件

### 页面验证

- 导入导出流程完整
- 任务状态展示正确

### 安全验证

- 上传文件类型白名单校验
- 导出权限仅管理员可用

## 11. 验收标准

- 导入导出可用
- 错误报告清晰
- 任务记录完整

## 12. 当前实现说明

### 实现范围

- 已实现管理员下载工具导入模板
- 已实现 CSV 内容预校验、同步导入和任务记录
- 已实现按字段选择导出工具数据并生成 CSV 文件
- 已实现管理端导入导出页，支持模板下载、预校验、执行导入、执行导出、查看任务记录

### 数据与接口设计

- 数据库迁移文件为 `backend/src/main/resources/db/migration/V8__m09_import_export.sql`
- `import_tasks` 与 `export_tasks` 补充了 `file_name`、`success_count`、`failure_count`、`error_report_path`、`detail`
- 管理侧接口为：
  - `GET /api/admin/import-export/template`
  - `POST /api/admin/import-export/imports/preview`
  - `POST /api/admin/import-export/imports`
  - `GET /api/admin/import-export/imports`
  - `POST /api/admin/import-export/exports`
  - `GET /api/admin/import-export/exports`
- 当前导出字段支持：
  - `name`
  - `summary`
  - `description`
  - `url`
  - `usageGuide`
  - `recommenderName`
  - `status`
  - `starCount`
  - `favoriteCount`
  - `commentCount`
  - `tags`
  - `updatedAt`

### 测试方案

- 单元/接口测试：`ImportExportControllerTests`
- 后端全量验证：`cd backend && mvn test`
- 前端校验：`cd frontend && npm run lint && npm run build`

### 验收结论

- 管理员已可完成模板下载、导入预校验、批量导入、字段导出和任务查看
- 非管理员访问管理侧导入导出接口会被拒绝
- 导入失败信息会落到任务详情中，便于排查问题
- 当前版本为 CSV 方案，已形成可验证闭环，可继续作为 `M10` 审计日志与 `M11` 联调的基础

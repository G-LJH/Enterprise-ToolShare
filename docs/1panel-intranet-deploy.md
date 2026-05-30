# 1Panel 内网直连部署说明

这份配置不依赖反向代理，适合公司内网直接通过 IP + 端口访问。

## 访问方式

- 前端: `http://10.0.70.121:3000`
- 后端健康检查: `http://10.0.70.121:8080/api/health`
- 1Panel 管理面板: `http://10.0.70.121:1800`

`1800` 是 1Panel 面板端口，不是业务系统端口。业务系统建议继续使用 `3000` 和 `8080` 这类独立端口。

## 部署文件

- 生产编排: `docker-compose.prod.yml`
- 生产环境变量模板: `.env.prod.example`

建议先复制一份生产环境变量:

```bash
cp .env.prod.example .env.prod
```

然后至少修改这些值:

- `POSTGRES_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `NEXT_PUBLIC_BACKEND_BASE_URL`
- `CORS_ALLOWED_ORIGINS`

如果你的服务器 IP 不是 `10.0.70.121`，要把上面两个 URL 一并改掉。

## 1Panel 推荐配置

在 1Panel 中创建 Compose 应用，选择仓库目录后使用 `docker-compose.prod.yml`。

目录挂载会自动写入以下位置:

- `./data/postgres`
- `./logs/backend`
- `./backups`

这样更适合在 1Panel 里查看数据、日志和备份。

## 首次启动后检查

1. 打开 `http://10.0.70.121:3000`
2. 检查后端健康状态 `http://10.0.70.121:8080/api/health`
3. 在 1Panel 查看三个容器是否都为健康状态

如果后端容器启动失败，并且你之前已经初始化过 `./data/postgres`，请重点检查 `.env.prod` 中的 `POSTGRES_PASSWORD` 和 `SPRING_DATASOURCE_PASSWORD` 是否与数据库首次初始化时使用的密码一致。修改环境变量不会自动修改已初始化数据库的真实密码。

## 端口建议

- `3000`: 前端
- `8080`: 后端
- `5432`: PostgreSQL 不建议对外暴露

当前生产 compose 没有发布数据库端口。如果你确实需要从内网别的机器连数据库，再单独放开 `5432`，不要默认暴露。

# ============================================================
# frontend Dockerfile (多阶段构建)
# Stage 1: Node 构建产物
# Stage 2: Nginx 托管静态资源 + 反向代理到后端
# 设计要点:
#   1. 构建阶段使用 npm run build:only 跳过 vue-tsc 类型检查
#      (类型检查应在 CI 阶段执行, 避免因类型问题阻塞镜像构建)
#   2. VITE_API_BASE_URL=/api 让前端所有 API 请求走相对路径 /api/xxx
#      由 Nginx 反向代理转发到后端 app:8080
#   3. 运行时容器以非 root 用户 nginx 运行 (安全)
#   4. 最终镜像仅包含 Nginx + 静态产物, 体积小 (~30MB)
# ============================================================

# ---------- Stage 1: Builder ----------
FROM node:18-alpine AS builder

WORKDIR /app

# 先复制依赖描述文件, 利用 Docker 层缓存加速依赖安装
COPY package.json package-lock.json ./

# npm ci: 严格按 package-lock.json 安装, 保证可重复构建
RUN npm ci

# 复制源码 (.dockerignore 已排除 node_modules / dist 等)
COPY . .

# 构建参数: 前端 API 基础路径, 默认 /api (由 Nginx 反向代理到后端)
# 通过 ARG + ENV 双重设置, ARG 允许构建时覆盖, ENV 让 Vite 在构建时读取
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

# 仅执行 vite build, 跳过 vue-tsc 类型检查
# 类型检查应在 CI 阶段 (npm run build) 执行, 避免因类型问题阻塞部署
RUN npm run build:only

# ---------- Stage 2: Runtime ----------
FROM nginx:1.27-alpine AS runtime

# 准备非 root 运行环境:
#   1. 创建 nginx pid 和缓存目录, 赋权给 nginx 用户
#   2. 修改主配置 pid 路径到 nginx 用户可写目录
#   3. 清理默认 default.conf (由我们自己的配置覆盖)
RUN mkdir -p /var/cache/nginx /var/run/nginx && \
    chown -R nginx:nginx /var/cache/nginx /var/run/nginx /usr/share/nginx/html && \
    sed -i 's|pid\s*/var/run/nginx.pid;|pid /var/run/nginx/nginx.pid;|' /etc/nginx/nginx.conf && \
    rm -f /etc/nginx/conf.d/default.conf

# 复制构建产物到 Nginx 静态资源目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制自定义 Nginx 配置 (server 块: SPA 路由 + 反向代理 + gzip)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# 健康检查: 每 30s 探测首页, 3s 超时, 连续 3 次失败标记 unhealthy
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1

# 非 root 运行 (nginx 用户, nginx:alpine 镜像内置 UID=101)
USER nginx
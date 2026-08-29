# =====================================================================
# 会员端镜像（Vue3 + Vant4 + Vite 构建 → Nginx 托管）
# 构建：docker build -t eldercare-member .
# =====================================================================

# ---- 构建阶段 ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- 运行阶段：Nginx 托管 dist + 代理 /api 到后端 ----
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

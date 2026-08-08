# =============================================================================
# wardrobe-front-user Dockerfile (多阶段构建)
#   Stage 1: Node.js 20 环境下 yarn install + yarn build
#   Stage 2: Nginx (Alpine) 托管生产产物 + 反代 /api 到后端容器
# =============================================================================

# --------------------------- Stage 1: Build ---------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# 使用阿里云 Alpine 镜像源加速 apk（临时用，仅 builder 阶段生效）
RUN sed -i 's|https://dl-cdn.alpinelinux.org/alpine|https://mirrors.aliyun.com/alpine|g' /etc/apk/repositories \
    && npm config set registry https://registry.npmmirror.com \
    && corepack enable >/dev/null 2>&1 || true \
    && (yarn config set registry https://registry.npmmirror.com >/dev/null 2>&1 || true)

# 先只拷贝依赖文件，缓存 node_modules 层 (yarn.lock / package-lock.json 任一即可)
COPY package.json ./
COPY yarn.lock* package-lock.json* ./
RUN --mount=type=cache,target=/usr/local/share/.cache/yarn/v6 \
    --mount=type=cache,target=/root/.npm \
    if [ -f package-lock.json ]; then \
      npm ci --no-audit --no-fund --loglevel=error || npm install --no-audit --no-fund --loglevel=error; \
    elif [ -f yarn.lock ]; then \
      yarn install --frozen-lockfile --non-interactive --network-timeout 600000 2>/dev/null || yarn install --non-interactive --network-timeout 600000; \
    else \
      npm install --no-audit --no-fund --loglevel=error; \
    fi

# 再拷贝源码 & 构建
COPY index.html ./
COPY public ./public
COPY src    ./src
COPY vite.config.ts tsconfig.json tsconfig.node.json tsconfig.app.json* ./
RUN if [ -f package-lock.json ]; then \
      NODE_OPTIONS="--max-old-space-size=4096" npm run build; \
    elif [ -f yarn.lock ]; then \
      NODE_OPTIONS="--max-old-space-size=4096" yarn build; \
    else \
      NODE_OPTIONS="--max-old-space-size=4096" npm run build; \
    fi

# --------------------------- Stage 2: Runtime -------------------------------
FROM nginx:1.27-alpine

# Alpine apk 阿里云镜像 + 东八区
ENV TZ=Asia/Shanghai
RUN sed -i 's|https://dl-cdn.alpinelinux.org/alpine|https://mirrors.aliyun.com/alpine|g' /etc/apk/repositories \
    && apk add --no-cache tzdata \
    && ln -sf /usr/share/zoneinfo/${TZ} /etc/localtime && echo ${TZ} > /etc/timezone

# 替换默认的 Nginx 配置（我们自己写的 SPA + 反代规则）
COPY nginx.conf /etc/nginx/nginx.conf

# 把构建好的 dist/ 放到 Nginx 静态根
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]

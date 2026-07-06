# syntax=docker/dockerfile:1

# ---- Stage 1: build the Vite bundle ----
FROM node:20-alpine AS build
WORKDIR /app

# Install dependencies against the lockfile first for better layer caching.
COPY package*.json ./
RUN npm ci

# Build the production bundle.
COPY . .
RUN npm run build

# ---- Stage 2: serve with nginx ----
FROM nginx:1.27-alpine AS serve

# SPA + gzip + caching + /api reverse proxy.
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1

CMD ["nginx", "-g", "daemon off;"]

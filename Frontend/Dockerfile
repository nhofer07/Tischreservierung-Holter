FROM --platform=$BUILDPLATFORM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/Frontend/browser /usr/share/nginx/html
COPY --chmod=755 docker-entrypoint.d/40-runtime-config.sh /docker-entrypoint.d/40-runtime-config.sh
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1

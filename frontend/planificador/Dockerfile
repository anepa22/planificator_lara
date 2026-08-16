# PRODUCCIÓN — Frontend multi-arch (linux/amd64 + linux/arm64)
# Vite build en BUILDPLATFORM; nginx final sigue TARGETPLATFORM.
# Subpath vía build-args (CI):
#   VITE_BASE, VITE_API_URL, VITE_APP_ENV, VITE_APP_VERSION, VITE_APP_GIT_SHA
FROM --platform=$BUILDPLATFORM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY index.html vite.config.js ./
COPY public ./public
COPY src ./src
ARG VITE_BASE=/dev/planificator/
ARG VITE_API_URL=/dev/planificator
ARG VITE_APP_ENV=prod
ARG VITE_APP_VERSION=0.1.0
ARG VITE_APP_GIT_SHA=local
ENV VITE_BASE=$VITE_BASE
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_APP_ENV=$VITE_APP_ENV
ENV VITE_APP_VERSION=$VITE_APP_VERSION
ENV VITE_APP_GIT_SHA=$VITE_APP_GIT_SHA
RUN npx vite build --mode prod

FROM nginx:1.27-alpine
COPY default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
ENV PLANIFICATOR_ENV=production

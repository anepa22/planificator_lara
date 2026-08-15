# PRODUCCIÓN — Frontend multi-arch (linux/amd64 + linux/arm64)
# Vite build en BUILDPLATFORM; nginx final sigue TARGETPLATFORM.
# Subpath vía build-args (CI):
#   dev:  VITE_BASE=/dev/planificator/  VITE_API_URL=/dev/planificator  VITE_APP_ENV=dev
#   test: VITE_BASE=/test/...          VITE_API_URL=/test/...          VITE_APP_ENV=test
#   prod: VITE_BASE=/                   VITE_API_URL=                   VITE_APP_ENV=prod
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
ENV VITE_BASE=$VITE_BASE
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_APP_ENV=$VITE_APP_ENV
RUN npx vite build --mode prod

FROM nginx:1.27-alpine
COPY default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
ENV PLANIFICATOR_ENV=production

# PRODUCCIÓN — Frontend multi-arch (linux/amd64 + linux/arm64)
# Vite build en BUILDPLATFORM; nginx final sigue TARGETPLATFORM.
# Subpath vía build-args (CI):
#   dev:  VITE_BASE=/dev/planificator/  VITE_API_URL=/dev/planificator
#   prod: VITE_BASE=/prod/planificator/ VITE_API_URL=/prod/planificator
FROM --platform=$BUILDPLATFORM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY index.html vite.config.js ./
COPY public ./public
COPY src ./src
ARG VITE_BASE=/dev/planificator/
ARG VITE_API_URL=/dev/planificator
ENV VITE_BASE=$VITE_BASE
ENV VITE_API_URL=$VITE_API_URL
RUN npx vite build --mode prod

FROM nginx:1.27-alpine
COPY default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
ENV PLANIFICATOR_ENV=production

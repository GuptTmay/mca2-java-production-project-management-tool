# --- Build stage ---
FROM node:20-alpine AS build
WORKDIR /app

# Vite inlines VITE_* variables at build time, not at container runtime -
# it must be passed as a build ARG, not a `docker run -e` / compose `environment:` value.
ARG VITE_API_BASE_URL=http://localhost:8081/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

COPY package.json package-lock.json* ./
RUN npm install

COPY . .
RUN npm run build

# --- Serve stage ---
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

FROM nginx:alpine

COPY target/gui-dist /usr/share/nginx/html
COPY src/main/docker/nginx.conf /etc/nginx/conf.d/default.conf

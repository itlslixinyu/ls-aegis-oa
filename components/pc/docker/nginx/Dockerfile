FROM docker.m.daocloud.io/library/nginx:1.27.0@sha256:98f8ec75657d21b924fe4f69b6b9bff2f6550ea48838af479d8894a852000e40 AS verified-assets
WORKDIR /work
COPY ls-aegis-web/ ls-aegis-web/
COPY docker/nginx/pc-inputs.sha256 docker/nginx/field-pc-assets.tgz ./
RUN sha256sum -c pc-inputs.sha256 >/dev/null && echo 'a0914b6d54ec8aa9e97403321e7e822998766b2a66b3c43a6309a81c2a2373ae  field-pc-assets.tgz' | sha256sum -c - && mkdir /output && tar -xzf field-pc-assets.tgz -C /output

FROM docker.m.daocloud.io/library/nginx:1.27.0@sha256:98f8ec75657d21b924fe4f69b6b9bff2f6550ea48838af479d8894a852000e40

COPY docker/nginx/conf/nginx.conf /etc/nginx/nginx.conf
COPY --from=verified-assets /output/ /usr/share/nginx/html/

ENV TZ=Asia/Shanghai
EXPOSE 80

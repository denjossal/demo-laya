FROM python:3.11-slim

WORKDIR /app

RUN pip install --no-cache-dir "laya[serve]==0.4.12"

ENV LAYA_HOST=0.0.0.0
ENV LAYA_PORT=8000
ENV LAYA_DEVICE=cpu
ENV LAYA_PRELOAD=1

EXPOSE 8000

CMD ["laya-serve"]

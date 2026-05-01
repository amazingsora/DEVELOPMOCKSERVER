services:
  mock-server:
    build:
      context: .
      dockerfile: server.dockerfile  # 強制指定檔名
    ports:
      - "8080:8080"
    volumes:
      - ./data:/data
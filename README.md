# 图域

## 项目简介

图域 是一个基于 Spring Boot 的后端项目，专注于图片数据处理和管理。

项目使用 Docker 容器化部署，集成了 MySQL、Redis 和 Elasticsearch 等常用中间件，提供了完整的数据存储、缓存和搜索功能。

使用腾讯云对象存储（COS）作为图片存储解决方案，确保图片数据的高可用性和安全性。
使用websocket实现实时通信聊天室，提升用户体验；以及协作编辑图片功能。
提供百度识图功能，方便用户快速查找相关图片。
内置sa-token进行用户认证和授权，确保系统的安全性。

项目采用模块化设计，业务代码位于 [src](file://E:/Java/JavaObject/graph%20domain/graph-domain-backend/src) 目录下，包含用户管理、图片处理、空间管理、搜索等多个业务模块，适用于构建图域相关的 Web 应用。

## 快速开始

### 环境要求

在开始之前，请确保你的系统已安装以下工具：

- [Docker](https://www.docker.com/)
- [Docker Compose](https://docs.docker.com/compose/)
- [Maven](https://maven.apache.org/)
- [Java 21](https://adoptium.net/)

### 启动步骤

按照以下步骤快速启动 Graph Domain Backend 服务：

1. 克隆项目到本地：
   ```bash
   git clone <repository-url>
   cd graph-domain-backend
   ```

2. 使用 Docker Compose 启动依赖服务：
   ```bash
   docker-compose up -d
   ```

   该命令将启动以下服务：
   - MySQL 8.0 数据库（端口: 3306）
   - Redis 7.0 缓存服务（端口: 6379）
   - Elasticsearch 8.0 搜索引擎（端口: 9200）

3. 构建并启动 Spring Boot 应用：
   ```bash
   mvn spring-boot:run
   ```

   或者先构建再运行：
   ```bash
   mvn clean package
   java -jar target/graph-domain-backend-0.0.1-SNAPSHOT.jar
   ```

4. 验证服务是否正常运行：
   ```bash
   docker-compose ps
   ```

   你可以看到所有服务的状态为 `Up`，表示服务已成功启动。

### 停止服务

如果你想停止所有服务，可以执行以下命令：

```bash
# 停止 Spring Boot 应用（如果在前台运行，直接 Ctrl+C 即可）
# 停止 Docker 容器
docker-compose down
```

这将停止并移除所有容器。

### 项目结构

项目主要业务模块包括：
- 用户管理 (user)
- 图片处理 (picture)
- 空间管理 (space)
- 搜索功能 (search)
- 评论系统 (comments)
- 点赞功能 (like)
- 消息中心 (messageCenter)
- 标签管理 (tag)
- 分类管理 (category)
- 积分系统 (points)
- 私聊功能 (privateChat)
- 关注系统 (userFollows)
- 空间分析 (spaceAnalyze)
- 空间用户管理 (spaceUser)
- 分享功能 (share)

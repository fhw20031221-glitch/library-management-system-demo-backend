# 图书管理系统 Demo - 后端

这是一个用于学习的图书管理系统后端项目，基于 Spring Boot、MyBatis-Plus、MySQL 和 JWT 实现。项目提供管理员和普通用户两类角色，支持图书管理、用户管理、借阅申请、审批、详情查看和归还登记等功能。

前端仓库：<https://github.com/fhw20031221-glitch/library-management-system-demo-frontend>

## 技术栈

- JDK 17
- Spring Boot 3.5.14
- Spring Security
- MyBatis-Plus 3.5.16
- MySQL 8.x
- JWT
- Maven Wrapper

## 功能说明

- 登录认证：使用 JWT Token 进行前后端分离鉴权。
- 角色权限：
  - `ADMIN`：图书管理、用户管理、借阅审批、申请详情、归还登记。
  - `READER`：浏览图书、提交借阅申请、查看自己的申请、归还登记。
- 图书管理：分页查询、新增、编辑、删除、库存维护。
- 用户管理：分页查询、新增、编辑、启用/禁用、重置密码。
- 借阅申请：提交申请、管理员审批、审批详情、归还后恢复库存。

## 目录结构

```text
src/main/java/com/example/demo
  common/      通用响应、异常、分页、常量
  config/      Spring Security、MyBatis-Plus 配置
  controller/  接口控制层
  dto/         请求参数对象
  entity/      数据库实体
  mapper/      MyBatis-Plus Mapper
  security/    JWT 与登录用户模型
  service/     业务逻辑
  vo/          接口返回对象
src/main/resources
  application.yml
  db/init.sql
```

## 本地运行

### 1. 初始化数据库

先启动 MySQL，并确保 `application.yml` 中的数据库账号密码可用：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/library_demo
    username: root
    password: 123456
```

导入初始化脚本：

```powershell
mysql -uroot -p123456 --default-character-set=utf8mb4 < src/main/resources/db/init.sql
```

如果本机没有安装 MySQL 客户端，也可以用 Docker 启动 MySQL 后再导入脚本。

### 2. 启动后端

```powershell
.\mvnw.cmd spring-boot:run
```

默认接口地址：

```text
http://localhost:8080
```

### 3. 默认账号

```text
管理员：admin / admin123
普通用户：reader / reader123
```

## 常用接口

```text
POST   /api/auth/login
GET    /api/auth/me
GET    /api/books
POST   /api/books
PUT    /api/books/{id}
DELETE /api/books/{id}
GET    /api/users
POST   /api/users
PUT    /api/users/{id}
PATCH  /api/users/{id}/status
PATCH  /api/users/{id}/password
POST   /api/borrow-applications
GET    /api/borrow-applications/my
GET    /api/borrow-applications
GET    /api/borrow-applications/{id}
PATCH  /api/borrow-applications/{id}/approve
PATCH  /api/borrow-applications/{id}/return
```

## 测试

```powershell
.\mvnw.cmd test
```

## 前端联调

前端默认通过 Vite 代理访问 `/api`，后端需要运行在 `8080` 端口。

CORS 已放行：

```text
http://localhost:5173
http://127.0.0.1:5173
```

## 说明

这是学习阶段 Demo 项目，配置中的数据库密码和 JWT 密钥仅用于本地演示。实际项目应使用环境变量或独立配置文件管理敏感信息。

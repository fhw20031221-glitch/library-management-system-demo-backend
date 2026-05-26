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
- Springdoc OpenAPI / Swagger UI
- Maven Wrapper

## 功能说明

- 登录认证：使用 JWT Token 进行前后端分离鉴权。
- 角色权限：
  - `ADMIN`：图书管理、用户管理、借阅审批、申请详情、归还登记。
  - `READER`：浏览图书、提交借阅申请、查看自己的申请、归还登记。
- 图书管理：分页查询、新增、编辑、删除、库存维护。
- 用户管理：分页查询、新增、编辑、启用/禁用、重置密码。
- 借阅申请：提交申请、管理员审批、审批详情、归还后恢复库存。

## 数据库设计

数据库名为 `library_demo`，初始化脚本位于 `src/main/resources/db/init.sql`。字符集使用 `utf8mb4`，存储引擎使用 InnoDB。

### 表关系

```text
user 1 ── N borrow_application N ── 1 book
```

- 一个用户可以提交多条借阅申请。
- 一本图书可以对应多条借阅申请。
- `borrow_application.user_id` 外键关联 `user.id`。
- `borrow_application.book_id` 外键关联 `book.id`。

### user 用户表

用于保存管理员和普通读者账户。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 登录用户名，唯一 |
| password | VARCHAR(100) | BCrypt 加密后的密码 |
| nickname | VARCHAR(50) | 昵称 |
| role | VARCHAR(20) | 角色：`ADMIN`、`READER` |
| status | VARCHAR(20) | 状态：`ENABLED`、`DISABLED` |
| phone | VARCHAR(30) | 手机号 |
| email | VARCHAR(100) | 邮箱 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

索引：

- `uk_user_username`：保证用户名唯一。

### book 图书表

用于保存图书基础信息和库存。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键，自增 |
| title | VARCHAR(100) | 书名 |
| author | VARCHAR(100) | 作者 |
| isbn | VARCHAR(30) | ISBN，唯一 |
| publisher | VARCHAR(100) | 出版社 |
| category | VARCHAR(50) | 分类 |
| total_stock | INT | 总库存 |
| available_stock | INT | 可借库存 |
| status | VARCHAR(20) | 状态：`NORMAL`、`DISABLED` |
| description | VARCHAR(500) | 图书简介 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

索引：

- `uk_book_isbn`：保证 ISBN 唯一。
- `idx_book_title`：用于书名查询。
- `idx_book_author`：用于作者查询。

### borrow_application 借阅申请表

用于保存借阅申请、审批和归还信息。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键，自增 |
| user_id | BIGINT | 申请人 ID |
| book_id | BIGINT | 图书 ID |
| reason | VARCHAR(500) | 申请说明 |
| status | VARCHAR(20) | 状态：`PENDING`、`APPROVED`、`REJECTED`、`RETURNED` |
| approval_comment | VARCHAR(500) | 审批意见 |
| borrow_date | DATE | 借出日期 |
| due_date | DATE | 应还日期 |
| return_date | DATE | 归还日期 |
| approved_at | DATETIME | 审批时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

索引：

- `idx_borrow_user_id`：用于查询某个用户的申请。
- `idx_borrow_book_id`：用于查询某本书的申请记录。
- `idx_borrow_status`：用于按审批状态筛选。

### 状态流转

```text
PENDING -> APPROVED -> RETURNED
PENDING -> REJECTED
```

- 普通用户提交申请后状态为 `PENDING`。
- 管理员审批通过后状态变为 `APPROVED`，同时扣减图书 `available_stock`。
- 管理员拒绝后状态变为 `REJECTED`，不改变库存。
- 已通过申请归还后状态变为 `RETURNED`，同时恢复图书 `available_stock`。

## 中间件和基础设施说明

本项目没有引入 Redis、消息队列、Elasticsearch 等额外中间件，学习阶段只依赖 MySQL 和后端应用本身。

### MySQL

- 用途：保存用户、图书和借阅申请数据。
- 数据库：`library_demo`
- 初始化脚本：`src/main/resources/db/init.sql`
- 连接配置：`src/main/resources/application.yml`

### Spring Security

- 用途：接口鉴权和角色权限控制。
- 登录接口 `/api/auth/login` 放行，其余业务接口需要携带 JWT。
- 管理员接口通过 `@PreAuthorize("hasRole('ADMIN')")` 控制。
- 普通用户只能访问自己的借阅申请，后端 Service 层会再次校验数据归属。

### JWT

- 用途：前后端分离登录状态维护。
- 前端登录成功后保存 Token，请求时通过 `Authorization: Bearer <token>` 传给后端。
- Token 中保存用户名、用户 ID 和角色。
- 过期时间由 `app.jwt.expiration-minutes` 配置，当前为 1440 分钟。

### MyBatis-Plus

- 用途：简化 CRUD、分页查询和条件构造。
- `PaginationInnerInterceptor` 用于分页。
- `MetaObjectHandler` 用于自动填充 `created_at` 和 `updated_at`。

### CORS

用于允许前端开发服务器访问后端接口，当前放行：

```text
http://localhost:5173
http://127.0.0.1:5173
```

### Springdoc OpenAPI / Swagger UI

- 用途：自动生成接口文档，并提供浏览器页面测试接口。
- 文档页面：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- 已配置 JWT Bearer 认证，登录成功后可在 Swagger 页面点击 `Authorize`，填写后端返回的 Token，再测试需要登录的接口。

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

接口测试页面：

```text
http://localhost:8080/swagger-ui.html
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

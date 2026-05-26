CREATE DATABASE IF NOT EXISTS library_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_demo;

DROP TABLE IF EXISTS borrow_application;
DROP TABLE IF EXISTS book;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(100) NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  phone VARCHAR(30) NULL,
  email VARCHAR(100) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE book (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  author VARCHAR(100) NOT NULL,
  isbn VARCHAR(30) NOT NULL,
  publisher VARCHAR(100) NULL,
  category VARCHAR(50) NULL,
  total_stock INT NOT NULL DEFAULT 0,
  available_stock INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL,
  description VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_book_isbn (isbn),
  KEY idx_book_title (title),
  KEY idx_book_author (author)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE borrow_application (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  book_id BIGINT NOT NULL,
  reason VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL,
  approval_comment VARCHAR(500) NULL,
  borrow_date DATE NULL,
  due_date DATE NULL,
  return_date DATE NULL,
  approved_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_borrow_user_id (user_id),
  KEY idx_borrow_book_id (book_id),
  KEY idx_borrow_status (status),
  CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES `user` (id),
  CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES book (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `user` (id, username, password, nickname, role, status, phone, email)
VALUES
  (1, 'admin', '$2b$10$Pgzse0bcrwRnaFr7Tgtqw.paDMZymLMvwl0rnNFVD/YSDliSZGU3a', '系统管理员', 'ADMIN', 'ENABLED', '13800000001', 'admin@example.com'),
  (2, 'reader', '$2b$10$yGHrAwUh1d9VzAuPijRKe.pme6RMSq679ZGL7U1yFkqDedxO1k62q', '普通读者', 'READER', 'ENABLED', '13800000002', 'reader@example.com');

INSERT INTO book (title, author, isbn, publisher, category, total_stock, available_stock, status, description)
VALUES
  ('Java核心技术', 'Cay S. Horstmann', '9787111636663', '机械工业出版社', '编程', 5, 5, 'NORMAL', 'Java基础与核心技术入门参考书。'),
  ('Spring Boot实战', 'Craig Walls', '9787115486981', '人民邮电出版社', '编程', 3, 3, 'NORMAL', 'Spring Boot Web开发学习用书。'),
  ('数据库系统概念', 'Abraham Silberschatz', '9787111375296', '机械工业出版社', '数据库', 2, 2, 'NORMAL', '数据库基础理论与实践参考书。'),
  ('深入浅出Vue.js', '刘博文', '9787115509055', '人民邮电出版社', '前端', 4, 4, 'NORMAL', 'Vue.js前端开发学习用书。');

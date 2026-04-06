# ResumeAI

ResumeAI 是一个前后端分离项目：
- 后端：Spring Boot + Spring AI + MyBatis-Plus + MySQL
- 前端：Vue 3 + Vite + Element Plus

## 目录结构

- `backend`：Java 后端服务
- `vue-frontend`：Vue 前端应用

## 环境要求

- JDK 17
- Maven 3.9+
- Node.js 18+
- npm 9+
- MySQL 8+

## 后端启动

1. 进入后端目录：

```bash
cd backend
```

2. 配置数据库与 AI Key（见 `src/main/resources/application.yml`）：

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.ai.openai.api-key`

推荐使用环境变量，不要把密钥直接写入仓库。例如：

```bash
AIHUBMIX_API_KEY=your_api_key
DB_URL=jdbc:mysql://localhost:3306/resumeai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=your_db_password
MAIL_HOST=smtp.example.com
MAIL_USERNAME=your_mail_user
MAIL_PASSWORD=your_mail_password
```

3. 启动后端：

```bash
mvn spring-boot:run
```

4. 默认端口：

- `9090`

## 前端启动

1. 进入前端目录：

```bash
cd vue-frontend
```

2. 安装依赖：

```bash
npm install
```

3. 启动开发环境：

```bash
npm run dev
```

4. 生产构建：

```bash
npm run build
```

## 本地联调建议

- 先启动 MySQL，并确保库表已初始化
- 启动后端（9090）
- 启动前端（Vite 默认 5173）
- 检查前端请求地址是否指向后端 9090

## 常见问题

- 如果 `npm install` 出现 audit 相关失败，可尝试：

```bash
npm install --no-audit
```


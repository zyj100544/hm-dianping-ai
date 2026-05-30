# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Heima Dianping (黑马点评) -- a local life services platform (shop reviews, blog posts, vouchers, flash sales) with AI-powered features. Three sub-projects form a polyglot system:

- **hm-dianping/** -- Java 8 / Spring Boot 2.3.12 backend (REST API, MySQL, Redis, RabbitMQ)
- **hm-dianping-web/** -- Vue 3.5 / Vite 8 frontend SPA
- **hm-dianping-ai-agent/** -- Python 3.13 / FastAPI AI sidecar (LLM via DeepSeek API)

## Running the Project

### Backend (port 8081)
```bash
cd hm-dianping
mvn spring-boot:run
# Windows with explicit JAVA_HOME:
$env:JAVA_HOME="D:\develop\jdk\jdk_1.8"; mvn spring-boot:run
```
Requires MySQL (127.0.0.1:3306, db `hmdp`), Redis (192.168.182.128:6379), RabbitMQ (192.168.182.128:5672). Schema + seed data in `hm-dianping/src/main/resources/db/hmdp.sql`.

### Frontend
```bash
cd hm-dianping-web
npm install
npm run dev
```
Vite dev server proxies `/api` to `http://localhost:8081`.

### AI Agent (port 8000)
```bash
cd hm-dianping-ai-agent
pip install -r requirements.txt
python web_server.py    # FastAPI endpoints (semantic search, blog assist)
python main.py          # RabbitMQ consumers (review audit, recommendations)
```

## Architecture

```
Browser -> hm-dianping-web (Vue 3 SPA)
              |
              v
hm-dianping (Spring Boot, port 8081)
  |-- MySQL (MyBatis-Plus ORM)
  |-- Redis (caching, distributed locks, Lua-based seckill)
  |-- RabbitMQ (async AI messaging)
  |-- Feign clients -> hm-dianping-ai-agent (sync AI calls)
              |
              v
hm-dianping-ai-agent (FastAPI, port 8000)
  |-- web_server.py: POST /api/semantic-search, /api/blog-assist
  |-- main.py: RabbitMQ consumers for review audit + recommendations
  |-- llm/: OpenAI SDK calls to DeepSeek API
```

### Backend (Java) - Spring Layered Architecture
- `controller/` -> `service/` (interface + impl) -> `mapper/` (MyBatis-Plus)
- `entity/` -- domain objects mapped to MySQL tables via MyBatis-Plus annotations
- `feign/` -- Feign clients for synchronous AI calls (AISearchFeignClient, AIBlogFeignClient)
- `consumer/` -- RabbitMQ consumers for async AI results (RecommendResultConsumer, ReviewAuditResultConsumer)
- `utils/CacheClient.java` -- cache-aside pattern with Redisson distributed locking
- `config/` -- MVC, Redis, MyBatis-Plus, Redisson configuration
- `interceptor/` -- token-based auth (LoginInterceptor + RefreshTokenInterceptor, Redis-stored tokens)
- `resources/seckill.lua` -- Redis Lua script for atomic flash sale stock deduction

### Frontend (Vue 3)
- `src/services/` -- Axios API wrappers (api.js, shop.js, blog.js, user.js, etc.)
- `src/views/` -- page components
- `src/router/index.js` -- Vue Router with routes for home, shops, blogs, search, recommendations, login
- `src/stores/` -- Pinia state management

### AI Agent (Python)
- `web_server.py` -- synchronous FastAPI endpoints called by Java Feign clients
- `main.py` -- entry point for async RabbitMQ consumers
- `consumer/rabbitmq_consumer.py` -- review audit consumer
- `consumer/recommend_consumer.py` -- personalized recommendation consumer
- `llm/openai_provider.py` -- LLM wrapper using OpenAI SDK against DeepSeek API
- `llm/prompt.py`, `llm/recommend_prompt.py` -- prompt templates
- `config.py` -- RabbitMQ connection + LLM settings (overridable via environment variables)

### Key Database Tables
`tb_shop`, `tb_shop_comments`, `tb_blog`, `tb_blog_comments`, `tb_user`, `tb_user_info`, `tb_voucher`, `tb_seckill_voucher`, `tb_voucher_order`, `tb_follow`, `tb_shop_type`

### RabbitMQ Topology
- Review audit: Java publishes to `review.audit.exchange` -> Python audits -> publishes to result queue -> Java consumes
- Recommendations: Java publishes to `recommend.exchange` -> Python ranks -> publishes to result queue -> Java consumes

## Configuration

- Backend: `hm-dianping/src/main/resources/application.yaml` (DB, Redis, RabbitMQ, AI endpoint URLs, Aliyun OSS)
- Frontend: `hm-dianping-web/.env` (API base URL, OSS static URL)
- AI Agent: `hm-dianping-ai-agent/config.py` (RabbitMQ, LLM settings -- all overridable via env vars like `LLM_API_KEY`, `RABBITMQ_HOST`)

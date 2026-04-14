# 2026-CSA-project

This repository is intended to become an AI-driven adaptive learning platform, but the current class-project stage only implements a minimal Gemini backend wrapper.

What is currently implemented:
- A minimal Java backend that accepts frontend messages and forwards them to Gemini
- A single `POST /api/chat` endpoint
- Basic error handling and environment-variable configuration
- Request/response documentation for frontend integration

What is intentionally out of scope right now:
- database
- user system
- adaptive testing logic
- student model
- analytics dashboard
- multi-model routing
- complex prompt orchestration

## Current Structure

```text
backend/
  .env.example
  API.md
  Application.java
  ChatHandler.java
  Config.java
  Gemini.java
  GeminiException.java
  HttpResponses.java
  Json.java
  run.sh
frontend/
  API_REQUEST.md
```

## Quick Start

Requirements:
- macOS
- JDK 21+
- A valid `GEMINI_API_KEY`

Run:

```bash
cd /Users/jeremyli/2026-CSA-project
export GEMINI_API_KEY="your_real_key"
export GEMINI_MODEL="gemini-3-flash-preview"
export PORT=8080
./backend/run.sh
```

Available endpoints after startup:
- `GET http://localhost:8080/health`
- `POST http://localhost:8080/api/chat`

## API Docs

- Backend API contract: [backend/API.md](/Users/jeremyli/2026-CSA-project/backend/API.md)
- Frontend API request guide: [frontend/API_REQUEST.md](/Users/jeremyli/2026-CSA-project/frontend/API_REQUEST.md)

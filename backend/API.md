# Backend API

## Purpose

At this stage, the backend only does one thing:

`Frontend -> Backend API -> Gemini -> Backend response -> Frontend`

It is a thin AI gateway, not a full learning-platform backend.

## Files

- App entry: [Application.java](/Users/jeremyli/2026-CSA-project/backend/Application.java)
- Request handler: [ChatHandler.java](/Users/jeremyli/2026-CSA-project/backend/ChatHandler.java)
- Gemini wrapper: [Gemini.java](/Users/jeremyli/2026-CSA-project/backend/Gemini.java)
- Config loader: [Config.java](/Users/jeremyli/2026-CSA-project/backend/Config.java)

## Endpoint

### `POST /api/chat`

Use this endpoint to send a single user message to the backend.

Request headers:

```http
Content-Type: application/json
```

Request body:

```json
{
  "message": "Explain derivatives simply"
}
```

Success response:

```json
{
  "reply": "A derivative measures how fast something is changing..."
}
```

Error response:

```json
{
  "error": "Gemini API call failed."
}
```

## Status Codes

- `200`: Request succeeded
- `400`: Invalid request body, invalid JSON, or empty input
- `500`: Server configuration issue, such as a missing `GEMINI_API_KEY`
- `502`: Gemini returned an invalid or empty result
- `504`: Network failure, timeout, or interrupted Gemini request

## Environment Variables

- `GEMINI_API_KEY`
  Required Gemini API key
- `GEMINI_MODEL`
  Optional model override
  Default: `gemini-3-flash-preview`
- `PORT`
  Optional backend port
  Default: `8080`

Example:

```bash
export GEMINI_API_KEY="your_real_key"
export GEMINI_MODEL="gemini-3-flash-preview"
export PORT=8080
./backend/run.sh
```

## Error Handling

The backend currently handles:
- Missing API key
- Empty input
- Invalid JSON
- Non-2xx Gemini responses
- Network failures
- Timeout or interrupted requests
- Empty Gemini output

The backend does not expose stack traces to the frontend.

## Run

```bash
cd /Users/jeremyli/2026-CSA-project
./backend/run.sh
```

The script:
1. Compiles all `.java` files directly under `backend/`
2. Outputs class files to `backend/out`
3. Starts the backend server

## Quick Debug Checklist

- Error: `Missing GEMINI_API_KEY environment variable.`
  Fix: export a valid key before starting the server

- Frontend gets `400`
  Fix: send a JSON object shaped like `{ "message": "..." }`

- Frontend gets `502`
  Fix: check the model name, API-key access, and Gemini response body

- Frontend gets `504`
  Fix: check network connectivity and retry the request

## Not Implemented Yet

- Multi-turn message history
- User accounts
- Stored chat history
- Adaptive learning logic
- Student modeling
- Prompt orchestration
- Analytics

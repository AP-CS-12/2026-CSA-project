# Frontend API Request Guide

## Current Flow

The frontend should send user input to the backend instead of calling Gemini directly.

Flow:

`Frontend -> /api/chat -> Backend -> Gemini -> Backend -> Frontend`

## Endpoint

- URL: `http://localhost:8080/api/chat`
- Method: `POST`
- Headers:

```http
Content-Type: application/json
```

## Request Body

```json
{
  "message": "Explain derivatives simply"
}
```

## Success Response

```json
{
  "reply": "..."
}
```

## Error Response

```json
{
  "error": "..."
}
```

## Fetch Example

```ts
const response = await fetch("http://localhost:8080/api/chat", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
  },
  body: JSON.stringify({
    message: userMessage,
  }),
});

const data = await response.json();

if (!response.ok) {
  throw new Error(data.error || "Chat request failed");
}

console.log(data.reply);
```

## Quick Integration Check

Send:

```json
{
  "message": "hello"
}
```

Expected result:
- On success: `reply`
- On failure: `error`

Frontend logic should simply do this:
- If `response.ok === true`, use `data.reply`
- If `response.ok === false`, use `data.error`

## Not Implemented Yet

- Multi-turn message arrays
- System prompt configuration
- User session binding
- Stored chat history
- Adaptive learning logic

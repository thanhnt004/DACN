# Frontend login wiring

- Pages: `src/pages/Login.tsx`
- API client: `src/api/http.ts`, `src/api/auth.ts`
- Store: `src/store/auth.ts`
- Routes updated in `src/App.tsx`

Environment

- VITE_API_BASE_URL (default: <http://localhost:8089>)

Run

1. Start backend on port 8089 and ensure CORS allows <http://localhost:5173> and cookies.
2. Start frontend: pnpm dev

Flow

- Login POST /api/v1/auth/login returns accessToken; refresh token is set as httpOnly cookie by backend.
- Axios attaches Authorization: Bearer accessToken; on 401 it calls /api/v1/auth/refresh and retries.

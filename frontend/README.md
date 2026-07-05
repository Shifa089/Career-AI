# CareerAI Frontend (Phase 6)

React 19 SPA for the CareerAI platform — resume analysis, AI mock interviews, and semantic job matching.

## Stack

- **React 19** + **Vite 6** + **TypeScript 5** (strict)
- **TailwindCSS 3.4** (brand palette, dark-mode ready)
- **TanStack React Query 5** (server state), **Zustand 5** (client state, persisted auth)
- **React Router 7**
- **Axios** (typed API client with JWT refresh interceptor)
- **@stomp/stompjs + sockjs-client** (interview WebSocket)
- **Recharts** (ATS gauge, skills, skill-gap, radar), **react-dropzone**, **react-hot-toast**, **@headlessui/react**, **lucide-react**, **date-fns**

## Getting started

```bash
npm install
cp .env.example .env   # optional; sensible dev defaults are baked in
npm run dev            # http://localhost:3000
```

The dev server proxies (see `vite.config.ts`):

| Path | Target | Notes |
|------|--------|-------|
| `/api` | `http://localhost:8080` | API Gateway (prefix `/api/v1`) |
| `/oauth2` | `http://localhost:8080` | OAuth2 authorization redirects |
| `/ws` | `http://localhost:8083` | Interview WebSocket (SockJS/STOMP) |

Start the backend first: `docker compose up -d` then the services (discovery → config → gateway → app services).

## Scripts

- `npm run dev` — dev server
- `npm run build` — type-check + production build
- `npm run lint` — ESLint
- `npm run preview` — preview the production build

## Backend contract notes

- All REST responses are wrapped in `ApiResponse<T>`; the axios interceptor unwraps `.data`.
- Paginated endpoints return a raw Spring `Page<T>` (`content`, `totalElements`, …).
- Resume upload returns **202** and analysis runs async — the UI polls `/analysis` until `ANALYSED`.
- The interview flow is fully WebSocket-driven (STOMP over SockJS).

> **Auth note:** `auth-service` was not yet implemented when this frontend was built. The auth API layer
> targets a standard JWT contract (`POST /auth/login|register|refresh` → `{ accessToken, refreshToken, user }`)
> consistent with the gateway's verified claim contract (`sub=email`, `userId`, `roles`, `type=ACCESS`).
> Adjust `src/api/auth.ts` / `src/types/index.ts` if the implemented contract differs.

## Structure

```
src/
├── api/          Axios instance + per-domain API functions
├── components/   common/ · resume/ · interview/ · jobs/
├── hooks/        React Query hooks + useWebSocket
├── pages/        Route-level pages
├── store/        Zustand stores (auth, interview)
├── types/        All TypeScript interfaces & enums
└── utils/        JWT + formatting helpers
```

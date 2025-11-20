# AirHotel Client

Single-page React app that consumes the AirHotel /hotels API and displays results Expedia-style: destination search, date pickers (for future use), and a booking CTA on each hotel card.

## Prerequisites

- Node.js 18+
- Running AirHotel backend exposing /hotels

## Getting Started

```bash
cd client_program
npm install
npm run dev
```

The dev server runs at <http://localhost:5173> and proxies `/hotels` and `/reservations` to `http://localhost:8080`. Leave `VITE_API_BASE_URL` unset to leverage this proxy so cookies remain same-origin. To target a different backend (e.g., staging/production), set `AIRHOTEL_API` or configure `VITE_API_BASE_URL` inside `.env` (see `.env.example`).

> **Session Cookie**
> Set VITE_SESSION_COOKIE=JSESSIONID=... in .env. The client automatically attaches it to every request.

## Production Build

```bash
npm run build
npm run preview
```

Deploy the generated `dist/` directory, ensuring `/hotels` and `/reservations` still point to your AirHotel service (either via reverse proxy or `VITE_API_BASE_URL`).

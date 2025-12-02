# AirHotel Client

Single-page React app for browsing hotels, checking room-type availability, and managing reservations against the AirHotel backend.

## Prerequisites
- Node.js 18+

## Getting Started
```bash
cd client_program
npm install
npm run dev
```
The dev server runs at <http://localhost:5173> and proxies `/hotels`, `/reservations`, `/logout`, and `/oauth2` to `http://localhost:8080`. Leave `VITE_API_BASE_URL` unset to use this proxy so cookies stay same-origin. To target another backend, set `AIRHOTEL_API` or `VITE_API_BASE_URL` in `.env` (see `.env.example`) and restart `npm run dev`.

## App Structure & How to Use
- **Sign in / out**: Top-right buttons. “Sign in” redirects to the backend OAuth entry. After the backend returns a response containing `session.jsessionId`, the app stores `JSESSIONID` in the browser; “Log out” calls `/logout`, clears local state (hotels, reservations, filters), and removes the cookie.
- **Search view**: Destination input, date range picker, and “Search Hotels.” Click a hotel’s **Reserve Now** to open the room-type modal. In the modal, pick a room type, and submit to create a reservation.
- **My Reservations view**: Cards grouped into Upcoming, Past, and Canceled. Only `pending`/`confirmed` reservations show **Modify** and **Cancel** buttons:
  - Modify opens a modal to change dates, then PATCH `/reservations/{id}`.
  - Cancel asks for confirmation, then DELETE `/reservations/{id}`.
- **Toasts**: Success/error notifications appear bottom-right for key actions.

## Sessions & Multiple Clients
- The backend issues `JSESSIONID` on successful OAuth login; the client stores it when a response includes `session.jsessionId`. All API calls send `credentials: 'include'`.
- Each browser/tab keeps its own cookie; multiple clients can interact simultaneously because the backend distinguishes them by their session.

## Production Build
```bash
npm run build
npm run preview
```
Deploy the generated `dist/` behind a reverse proxy that forwards `/hotels`, `/reservations`, `/logout`, and `/oauth2` to your AirHotel backend.

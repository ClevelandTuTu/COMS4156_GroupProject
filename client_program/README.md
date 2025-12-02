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

## Manual Tests (UI → API → Expected Response)
- **Sign in**: click “Sign in” → browser goes to `/oauth2/authorization/google` (redirect flow). On success, backend responds with JSON containing `session.jsessionId`; cookie `JSESSIONID` is then set to the browser.
- **Log out**: click “Log out” → `POST /logout` with cookies → `200 OK`, JSON `{"message":"Logged out successfully"}`; client clears local state.
- **Search Hotels**: button on Search view → `GET /hotels` → `200 OK`, body is an array of hotel objects (id, name, brand, address*, city, country, starRating, createdAt…).
- **Room type availability**: in “Reserve Now” modal, enter guests → `GET /hotels/{hotelId}/room-types/availability?checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD&numGuests=N` → `200 OK`, array of room types `{roomTypeId, code, name, bedType, capacity, totalRooms, available, baseRate}`.
- **Create reservation**: modal “Submit” → `POST /reservations` with JSON `{hotelId, roomTypeId, checkInDate, checkOutDate, nights, numGuests, currency:"USD", priceTotal, notes:""}` → `200 OK`, body is reservation JSON; client then refetches reservations.
- **Modify reservation**: “Modify” on a `pending`/`confirmed` card → `PATCH /reservations/{id}` with `{checkInDate, checkOutDate, nights}` → `200 OK`, body is updated reservation; client refetches list.
- **Cancel reservation**: “Cancel” on a `pending`/`confirmed` card → `DELETE /reservations/{id}` → `200 OK` (body may be empty/JSON message); client refetches list.

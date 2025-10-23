# Airhotel Application

## 1. Overview

Our team develops a Hotel Booking and Upgrade Management Service.  

This service provides an API for managing hotel room reservations from multiple sources, 
such as the hotel’s own website (e.g. Hilton.com, Marriott.com and Hyatt.com), 
OTA platforms (e.g. Expedia and Booking.com), and internal Property Management System.   

In addition, the service will perform useful computations to calculate priority for room upgrades for 
frequent travelers (All hotel groups like Hilton and Marriott have such a system), 
ensuring that high-value customers receive appropriate benefits.

**Functionality:**  

- Accept booking requests from multiple client systems and store them in a persistent datastore. 
- Manage cancellations, modifications, and room assignments. 
- Calculate upgrade priority for frequent travelers based on criteria such as membership tier, past booking history, spending level, and room availability. (beyond CRUD)
- Optimize room assignment when multiple high-tier customers compete for limited upgraded room types. (beyond CRUD)
- Adjust room rates dynamically based on factors such as seasonality, holidays, real-time occupancy, and booking trends, ensuring optimized revenue and competitive pricing. (beyond CRUD)
- Estimate overbooking risk for specific dates by analyzing historical cancellation and no-show rates, enabling hotels to balance occupancy maximization with guest satisfaction. (beyond CRUD)
- Log all API calls (reservations, cancellations, upgrade requests) with timestamp, client ID, and request details.


**Persistent Data:** 

- Hotel room inventory (room types, availability, pricing). 
- Customer profiles (basic info, membership tier, loyalty points). 
- Reservation records (status, dates, source of booking). 
- Upgrade history and allocation outcomes. 
- API access logs.

---

## 2. Instruction on Build and Run our application

---

## 3. API Endpoints

# Hotel Booking API

> **Auth**
> - **Public** endpoints do not require authentication.
> - **Private** endpoints require `Authorization: Bearer <token>`.

---

## Public

### /hotels
| Method | Path                     | Description                     |
|-------:|--------------------------|---------------------------------|
| GET    | `/hotels`                | List all hotels.                |
| GET    | `/hotels/{id}`           | Fetch hotel details.            |
| GET    | `/hotels/{id}/rooms`     | Available rooms in that hotel.  |

### /auth
| Method | Path                         | Description                                |
|-------:|------------------------------|--------------------------------------------|
| GET    | `/auth/google/login`         | Redirect to Google OAuth.                  |
| GET    | `/auth/google/rollback`      | Google redirects here with `code`.         |
| GET    | `/auth/me`                   | Return current logged-in user info.        |

---

## Private

### /reservations
(Authenticated — current user context)

| Method | Path                           | Description                 |
|-------:|--------------------------------|-----------------------------|
| GET    | `/reservations`                | List current user’s reservations. |
| POST   | `/reservations`                | Create a reservation.       |
| GET    | `/reservations/{id}`           | Fetch reservation details.  |
| PATCH  | `/reservations/{id}`           | Modify reservation.         |
| DELETE | `/reservations/{id}`           | Cancel reservation.         |

---

## Manager (Private)

### /manager/hotels/{hotelId}/rooms
| Method | Path                                                  | Description        |
|-------:|-------------------------------------------------------|--------------------|
| GET    | `/manager/hotels/{hotelId}/rooms`                     | List/filter rooms. |
| POST   | `/manager/hotels/{hotelId}/rooms`                     | Create room.       |
| DELETE | `/manager/hotels/{hotelId}/rooms/{roomId}`            | Delete room.       |
| PATCH  | `/manager/hotels/{hotelId}/rooms/{roomId}`            | Update room.       |

### /manager/hotels/{hotelId}/reservations
| Method | Path                                                                 | Description                        |
|-------:|----------------------------------------------------------------------|------------------------------------|
| GET    | `/manager/hotels/{hotelId}/reservations`                             | List/filter all reservations.      |
| GET    | `/manager/hotels/{hotelId}/reservations/{reservationId}`             | View any guest’s reservation.      |
| PATCH  | `/manager/hotels/{hotelId}/reservations/{reservationId}`             | Modify reservation.                |
| PATCH  | `/manager/hotels/{hotelId}/reservations/{reservationId}:apply-upgrade` | Apply upgrade.                     |
| PATCH  | `/manager/hotels/{hotelId}/reservations/{reservationId}:check-in`    | Check in.                          |
| PATCH  | `/manager/hotels/{hotelId}/reservations/{reservationId}:check-out`   | Check out.                         |
| DELETE | `/manager/hotels/{hotelId}/reservations/{reservationId}`             | Cancel reservation.                |

---

## Admin

### /admin
| Method | Path                     | Description          |
|-------:|--------------------------|----------------------|
| POST   | `/admin/pricing/update`  | Trigger pricing update. |

---

## 4. Steps to test private API Endpoints

1. Go to (https://airhotelv1-364322827659.us-east4.run.app/login)
2. Sign in with your Google account
3. Open the develop console in your browser
4. Go to Application tab
5. Under Cookie, copy the value
6. In the header section of postman (or similar API testing tools), add a key (Cookie), and a value (JSESSIONID="The cookie value you got")
7. With the cookie in the header section, you now have access to private endpoints

--- 


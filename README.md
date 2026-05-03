# 🎬 CineBook — Movie Ticket Booking System
### OOAD Mini Project | PES University | Spring Boot + React + MongoDB

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- MongoDB Community (local)
- Node.js 18+

---

### Step 1 — Start MongoDB
```powershell
net start MongoDB
```

### Step 2 — Run Spring Boot Backend
```powershell
cd "C:\Users\sdurg\Downloads\moviebooking (1)\moviebooking"
.\mvnw.cmd spring-boot:run
```
Backend runs at: **http://localhost:8080**

### Step 3 — Run React Frontend
```powershell
cd "C:\Users\sdurg\Downloads\moviebooking (1)\moviebooking\frontend"
npm run dev
```
Frontend runs at: **http://localhost:3000**

---

## 📂 Project Structure

```
moviebooking/
├── src/main/java/com/project/moviebooking/
│   ├── model/          ← 9 MongoDB document classes
│   ├── repository/     ← 8 MongoDB repository interfaces
│   ├── service/        ← 6 service classes (business logic)
│   ├── controller/     ← 7 REST controllers
│   ├── dto/            ← 6 request/response DTOs
│   ├── config/         ← JWT + Spring Security config
│   ├── patterns/       ← All 4 design patterns
│   └── exception/      ← Global exception handler
├── src/main/resources/
│   └── application.properties
├── frontend/           ← React + Vite frontend
│   └── src/
│       ├── pages/      ← 10 page components
│       ├── components/ ← Navbar, ProtectedRoute, Toast
│       └── api/        ← Axios instance with JWT interceptor
└── mongodb_seed.js     ← Sample data
```

---

## 🎯 REST API Reference

### Auth APIs (Public)
| Method | Endpoint | Body | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | `{name,email,password,phone,role}` | Register user |
| POST | `/api/auth/login` | `{email,password}` | Login → JWT token |

### Movie APIs (Public GET, Admin POST/PUT/DELETE)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/movies` | All active movies |
| GET | `/api/movies/{id}` | Single movie |
| GET | `/api/movies/search?title=` | Search by title |

### Show APIs (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/shows/movie/{movieId}` | Shows for a movie |
| GET | `/api/shows/{showId}/seats` | Seat grid for seat selection |

### Booking APIs (JWT Required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/bookings` | Create booking (lock seats) |
| GET | `/api/bookings/my` | User booking history |
| POST | `/api/bookings/{id}/cancel` | Cancel booking |

### Payment APIs (JWT Required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments/process` | Pay → confirm → issue ticket |

### Ticket APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tickets/{ticketId}` | Get ticket |
| GET | `/api/tickets/my` | User's all tickets |

### Admin APIs (`role: ADMIN` required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/admin/movies` | Add movie |
| PUT | `/api/admin/movies/{id}` | Update movie |
| DELETE | `/api/admin/movies/{id}` | Delete movie |
| POST | `/api/admin/shows` | Create show (auto-generates 150 seats) |
| POST | `/api/admin/theatres` | Add theatre |
| GET | `/api/admin/bookings` | View all bookings |

---

## 🏗️ Design Patterns Implemented

### 1. 🔵 Singleton (Creational)
**File:** `patterns/MongoDBSingleton.java`
- Thread-safe singleton for MongoDB connection
- Double-checked locking for performance
- Spring `@Component` enforces single instance via IoC

### 2. 🟠 Adapter (Structural)
**File:** `patterns/PaymentGatewayAdapter.java`
- Adapts Razorpay, Stripe, UPI into unified `PaymentGateway` interface
- New gateways added without changing existing code

### 3. 🟢 Observer (Behavioral)
**File:** `patterns/BookingNotificationSubject.java`
- Subject notifies Email, SMS, Push observers on booking events
- Loose coupling — Subject doesn't know observer count

### 4. 🟣 Factory (Creational)
**File:** `patterns/TicketFactory.java`
- Creates REGULAR / PREMIUM / VIP tickets based on seat row
- Client (BookingService) only requests type, not construction details

### 5. 🔴 Strategy (Behavioral — OCP)
**Files:** `patterns/PaymentStrategy.java` + UPI/Card/Wallet implementations
- Each payment method is a separate class
- Open for extension: add new method = new class, zero changes to existing

---

## 🧱 SOLID Principles

| Principle | Where |
|-----------|-------|
| **S** — SRP | `AuthService` (only auth) vs `UserProfileService` (only profile) |
| **O** — OCP | `PaymentStrategy` interface — extend without modification |
| **L** — LSP | `BaseUser` abstract class — User substitutes safely |
| **I** — ISP | `UserRepository`, `MovieRepository` — separate interfaces |
| **D** — DIP | All controllers inject service abstractions, not concrete classes |

---

## 👥 Team Contributions

| Member | Module | Endpoints |
|--------|--------|-----------|
| **Prasad** | Auth + User Management | `/api/auth/**` |
| **Akshaya** | Movie + Theatre + Show CRUD | `/api/movies/**`, `/api/shows/**`, `/api/theatres/**` |
| **Gowrish** | Seat Selection + Booking | `/api/shows/{id}/seats`, `/api/bookings/**` |
| **Harsha** | Payment + Ticket + Cancellation | `/api/payments/**`, `/api/tickets/**` |

---

## 🧪 Postman Quick Test

### Register Admin:
```json
POST http://localhost:8080/api/auth/register
{
  "name": "Admin User",
  "email": "admin@cinebook.com",
  "password": "admin123",
  "role": "ADMIN"
}
```

### Login:
```json
POST http://localhost:8080/api/auth/login
{
  "email": "admin@cinebook.com",
  "password": "admin123"
}
→ Copy the "token" from response
```

### Add Movie (use token):
```
POST http://localhost:8080/api/admin/movies
Authorization: Bearer <your-token>
{
  "title": "Pushpa 2",
  "genre": "Action",
  "language": "Telugu",
  "durationMinutes": 190,
  "rating": 8.0,
  "certificate": "UA"
}
```

---

## ✅ MongoDB Verification (MongoDB Compass)

Connect to: `mongodb://localhost:27017`
Database: `moviebooking`
Collections to check after testing:
- `users` — registered users
- `movies` — added movies
- `shows` — created shows
- `seats` — auto-generated per show (150 per show)
- `bookings` — confirmed bookings (status: CONFIRMED)
- `payments` — payment records (status: SUCCESS)
- `tickets` — issued tickets (code: REG-/PRE-/VIP-)

# 🎬 CineBook — Movie Ticket Booking System

> **OOAD Mini Project · PES University**
> A full-stack, production-grade movie ticket booking platform built with Spring Boot, React, and MongoDB.

---

## Overview

CineBook is an end-to-end online movie ticket reservation system. Users can browse movies, choose shows, select seats on a real-time visual seat matrix, pay via UPI / Card / Wallet, receive an e-ticket, and cancel with an IST-aware refund policy. Admins manage movies, theatres, shows, seat pricing, and view all bookings and users through a dedicated dashboard.

The project demonstrates five Gang-of-Four / GRASP design patterns, SOLID principles throughout, and a clean separation between the Spring Boot REST backend and the Vite-powered React frontend.

---

## Features

### User Features
- **Browse & Search** — filter movies by title or genre; poster images fetched from TMDB API
- **Show Selection** — shows grouped by date, IST-filtered (past shows hidden automatically)
- **Seat Selection** — live 10x15 seat grid (rows A-J, 15 columns = 150 seats/show) with colour-coded VIP / PREMIUM / REGULAR tiers
- **Atomic Booking** — all-or-nothing seat locking; maximum 8 seats per booking
- **Payment** — UPI, Card, and Wallet strategies with GST breakdown (12% / 18% per Indian Cinema Tax Rules)
- **E-Ticket** — auto-generated ticket with unique code prefix (VIP- / PRE- / REG-) after successful payment
- **Booking History** — view all past and active bookings with cancellation support
- **IST-Aware Refund** — 50% refund if cancelled >2 hours before show; no refund within 2 hours; GST is always non-refundable

### Admin Features
- Full CRUD for Movies, Theatres, and Shows
- Soft-delete for movies (set active=false); hard-delete also available
- Seat price update cascades to all unbooked seats when show price changes
- View all bookings with user / movie / theatre enrichment
- View all registered users

### Automated Operations
- **DataLoader** — on startup, seeds theatres, movies (TMDB or curated fallback), shows, and seats if the database is empty (idempotent)
- **ShowRefreshService** — @Scheduled cron fires at midnight IST to auto-generate next-day shows; 3 slots per movie per theatre (Morning 10:00 / Afternoon 14:00 / Evening 18:00)
- **Stale PENDING cleanup** — when a user creates a new booking for a show, any old PENDING bookings for the same show are automatically voided and seats released

---

## Frontend

### Tech Stack

| Tool | Version | Purpose |
|---|---|---|
| **React** | 18.3.1 | Component-based UI |
| **React Router DOM** | 7.14.1 | Client-side routing |
| **Axios** | 1.15.0 | HTTP client with JWT interceptor |
| **Vite** | 8.0.4 | Build tool and dev server |
| @vitejs/plugin-react | 4.3.1 | React Fast Refresh |

> All versions verified from frontend/package.json.

### Frontend Scripts

```bash
# Start development server (default: http://localhost:5173)
npm run dev

# Build production bundle
npm run build

# Preview production build locally
npm run preview
```

> **Note on ports:** Vite defaults to http://localhost:5173. The backend CORS configuration accepts any localhost:[*] port, so no CORS errors occur on port 5173 or 3000.

### Pages and Routes

| Route | Component | Auth Required |
|---|---|---|
| / | LandingPage | Public |
| /user-login | UserLogin | Public |
| /user-register | UserRegister | Public |
| /admin-login | AdminLogin | Public |
| /movies | MovieList | USER |
| /movies/:movieId/shows | ShowSelection | USER |
| /shows/:showId/seats | SeatSelection | USER |
| /payment/:bookingId | PaymentPage | USER |
| /ticket/:ticketId | TicketPage | USER |
| /my-bookings | BookingHistory | USER |
| /admin | AdminDashboard | ADMIN |

### Key Components

- **ProtectedRoute** — redirects unauthenticated users; enforces ADMIN role where required
- **Navbar** — top navigation bar
- **Toast** — non-blocking notification toasts
- **CancelBookingModal** — confirmation modal with refund preview

### Axios Configuration

The frontend Axios instance (src/api/axios.js) uses a relative base URL so Vite's proxy (/api to http://localhost:8080) handles all API calls. JWT tokens are attached automatically via a request interceptor. A response interceptor clears localStorage and redirects to / on any 401 Unauthorized response.

---

## Backend

### Tech Stack

| Tool | Version | Purpose |
|---|---|---|
| **Java** | 17 | Target JDK |
| **Spring Boot** | 3.3.5 | Application framework |
| **Spring Web MVC** | (via Boot) | REST controllers |
| **Spring Security** | (via Boot) | JWT stateless auth, BCrypt |
| **Spring Data MongoDB** | (via Boot) | Repository abstraction |
| **Spring WebFlux** | (via Boot) | WebClient for TMDB API calls |
| **jjwt-api / impl / jackson** | 0.11.5 | JWT token generation and parsing |
| **Lombok** | 1.18.36 | Boilerplate reduction |
| **Jakarta Bean Validation** | (via Boot) | @Valid, @NotBlank, @Email |
| **Maven** | (wrapper mvnw) | Build and dependency management |

> All versions verified from pom.xml.

### Run Command

```bash
# Windows PowerShell (from project root)
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Backend starts on **http://localhost:8080** (verified from application.properties and the startup banner in MoviebookingApplication.java).

### Package Structure

```
src/main/java/com/project/moviebooking/
├── MoviebookingApplication.java   # @SpringBootApplication, @EnableAsync, @EnableScheduling
├── config/
│   ├── DataLoader.java            # @PostConstruct -- seeds DB on startup
│   ├── JwtAuthFilter.java         # JWT filter before UsernamePasswordAuthenticationFilter
│   ├── JwtUtil.java               # Token generation, extractEmail, extractRole, validate
│   └── SecurityConfig.java        # CORS, route permissions, BCryptPasswordEncoder
├── controller/
│   ├── AuthController.java        # /api/auth/**
│   ├── MovieController.java       # /api/movies/**
│   ├── ShowController.java        # /api/shows/**
│   ├── TheatreController.java     # /api/theatres/**
│   ├── BookingController.java     # /api/bookings/**
│   ├── PaymentController.java     # /api/payments/**
│   ├── TicketController.java      # /api/tickets/**
│   └── AdminController.java       # /api/admin/** (ADMIN only)
├── model/
│   ├── BaseUser.java, User.java, Movie.java, Theatre.java
│   ├── Show.java, Seat.java, Booking.java, Payment.java, Ticket.java
├── repository/                    # Spring Data MongoDB interfaces
├── service/
│   ├── ISTTimeService.java        # All IST time logic (GRASP Information Expert)
│   ├── RefundService.java         # Cancellation refund policy (50% if >2h before show)
│   ├── ShowRefreshService.java    # @Scheduled midnight cron -- auto-generates next-day shows
│   └── impl/
└── patterns/                      # OOAD design pattern implementations (19 files)
    ├── AbstractTicket, RegularTicket, PremiumTicket, VIPTicket, TicketFactory
    ├── PaymentStrategy (interface), UPI/Card/WalletPaymentStrategy, PaymentContext
    ├── PaymentGateway (interface), ExternalPaymentAPI, PaymentGatewayAdapter
    ├── BookingNotificationSubject, BookingEventPublisher, BookingConfirmedEvent
    ├── EmailNotificationListener, SMSNotificationListener
    └── MongoDBSingleton
```

### Security Rules (verified from SecurityConfig.java)

| Path Pattern | Method | Auth Required |
|---|---|---|
| /api/auth/** | any | Public |
| /api/movies/** | GET | Public |
| /api/shows/** | GET | Public |
| /api/theatres/** | GET | Public |
| /api/tickets/** | GET | Public |
| /api/admin/** | any | ADMIN role only |
| Everything else | any | Authenticated user |

---

## Database

### Engine

**MongoDB** — document-oriented NoSQL database.

- **Connection URI:** mongodb://localhost:27017/moviebooking
- **Database name:** moviebooking
- **Spring property:** spring.data.mongodb.uri

> Verified from src/main/resources/application.properties.

### Collections

| Collection | Key Fields | Notes |
|---|---|---|
| users | name, email, password (BCrypt), phone, role (USER/ADMIN), active | Password never returned in API responses |
| theatres | name, location, city, rows, columns, totalSeats, active | totalSeats = rows x columns |
| movies | title, genre, language, durationMinutes, director, cast[], posterUrl, rating, releaseDate, certificate, active | active=false = soft deleted |
| shows | movieId, theatreId, showDate, showTime, price, availableSeats, bookedSeats[], active | 150 seats auto-generated on creation |
| seats | showId, theatreId, seatNumber, row, type (VIP/PREMIUM/REGULAR), price, isBooked, bookedByUserId | Rows A-B = VIP (2x), C-E = PREMIUM (1.5x), F-J = REGULAR (1x) |
| bookings | userId, showId, movieId, theatreId, seatNumbers[], totalAmount, bookingStatus (PENDING/CONFIRMED/CANCELLED), bookingTime (IST) | |
| payments | bookingId, paymentMethod, baseAmount, gstPercent, gstAmount, totalAmount, status, refundAmount, refundStatus, refundTime | GST is non-refundable |
| tickets | bookingId, userId, ticketCode, movieTitle, theatreName, showDate, showTime, seatNumbers[], totalAmount, status (VALID/CANCELLED), issuedAt | Ticket code prefix: VIP-, PRE-, REG- |

### Seat Pricing Logic

When a show is created with base price P:

| Row | Type | Multiplier | Price |
|---|---|---|---|
| A, B | VIP | 2.0x | 2P |
| C, D, E | PREMIUM | 1.5x | 1.5P |
| F, G, H, I, J | REGULAR | 1.0x | P |

If an admin updates the show price, all **unbooked** seats are repriced proportionally.

### Seed Data Notes

The mongodb_seed.js file is a reference-only script (run via MongoDB shell / Compass). The application is **self-seeding**: DataLoader.java runs @PostConstruct and calls ShowRefreshService.ensureShowsExist() to populate everything automatically at startup.

**Quirks to be aware of:**
1. BCrypt hashes in mongodb_seed.js are example hashes — always register users through POST /api/auth/register to get correct hashes.
2. The seed script does not create shows or seats — those are generated by DataLoader / ShowRefreshService at startup.
3. Default show timings: **10:00** (Morning, Rs.150), **14:00** (Afternoon, Rs.200), **18:00** (Evening, Rs.250).
4. DataLoader seeds 10 Bengaluru theatre locations (PVR, INOX, etc.).

---

## API Reference

All responses follow a standard envelope:
```json
{ "success": true, "message": "...", "data": { ... } }
```

### Authentication -- /api/auth

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | /api/auth/register | Public | Register a new user. Body: { name, email, password, phone, role }. Returns safe user DTO (no password). |
| POST | /api/auth/login | Public | Authenticate and get JWT. Body: { email, password }. Returns token, role, userId. |

### Movies -- /api/movies

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | /api/movies | Public | List all active movies |
| GET | /api/movies/{id} | Public | Get single movie by ID |
| GET | /api/movies/search?title=X | Public | Case-insensitive title search (active movies only) |
| GET | /api/movies/genre/{genre} | Public | Filter active movies by genre |

### Shows -- /api/shows

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | /api/shows/movie/{movieId} | Public | IST-filtered bookable shows grouped by date |
| GET | /api/shows/{showId} | Public | Single show with IST fields (displayTime, status, minutesLeft, bookable, isSoldOut) |
| GET | /api/shows/{showId}/seats | Public | Seat grid grouped by row (A-J). Blocked if show has already started. |

### Theatres -- /api/theatres

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | /api/theatres | Public | All theatres |
| GET | /api/theatres/{id} | Public | Single theatre by ID |
| GET | /api/theatres/city/{city} | Public | Theatres filtered by city (case-insensitive) |

### Bookings -- /api/bookings (all require JWT)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | /api/bookings | USER | Create booking. Body: { showId, seatNumbers[] }. Max 8 seats. Returns PENDING booking. |
| GET | /api/bookings/my | USER | Current user's bookings (newest first) |
| GET | /api/bookings/{id} | USER | Single booking by ID |
| GET | /api/bookings/{bookingId}/ticket | USER | Ticket associated with a booking |
| PUT | /api/bookings/{id}/cancel | USER | Cancel booking. IST-aware refund. Releases seats. |
| POST | /api/bookings/{id}/cancel | USER | Alias for PUT cancel (frontend convenience) |

### Payments -- /api/payments

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | /api/payments/process | USER | Process payment. Body: { bookingId, paymentMethod, upiId?, cardNumber? }. Applies GST, confirms booking, generates ticket. |
| GET | /api/payments/my | USER | Current user's payment history |
| GET | /api/payments/booking/{bookingId} | USER | Payment details with full GST breakdown |

### Tickets -- /api/tickets

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | /api/tickets/{ticketId} | Public | Get ticket by ticket ID |
| GET | /api/tickets/booking/{bookingId} | Public | Get ticket by booking ID |
| GET | /api/tickets/my | USER | All tickets for logged-in user |
| GET | /api/tickets/admin/all | ADMIN | All tickets (admin view) |

### Admin -- /api/admin (all require ADMIN role)

| Method | Path | Purpose |
|---|---|---|
| POST | /api/admin/movies | Add a new movie |
| GET | /api/admin/movies | List all movies (including inactive) |
| PUT | /api/admin/movies/{id} | Update movie fields (partial update) |
| DELETE | /api/admin/movies/{id} | Soft-delete movie (active=false) |
| DELETE | /api/admin/movies/{id}/hard | Hard-delete movie (permanent) |
| POST | /api/admin/theatres | Add a new theatre |
| GET | /api/admin/theatres | List all theatres |
| PUT | /api/admin/theatres/{id} | Update theatre fields |
| DELETE | /api/admin/theatres/{id} | Soft-delete theatre |
| POST | /api/admin/shows | Add a show. Body: { movieId, theatreId, showDate, showTime, price }. Auto-generates 150 seats. |
| GET | /api/admin/shows | All shows enriched with movie/theatre name and booked count |
| PUT | /api/admin/shows/{id} | Update show. Price change recalculates all unbooked seat prices. |
| DELETE | /api/admin/shows/{id} | Soft-delete show; deletes all associated seats |
| PUT | /api/admin/seats/{seatId} | Update individual seat type or price |
| GET | /api/admin/bookings | All bookings (enriched with user, movie, show, theatre) |
| GET | /api/admin/users | All registered users |

---

## External APIs and Integrations

### TMDB (The Movie Database) API
- **Purpose:** Fetch real movie metadata and poster images at startup
- **Client:** Spring WebFlux WebClient (configured in DataLoader.java)
- **Fallback:** If tmdb.api.key is unconfigured or request fails, DataLoader seeds a curated list of Indian movies with static poster URLs
- **Config property:** tmdb.api.key

### Indian Cinema Tax Engine
- **GST rate:** 12% for tickets <= Rs.100 base price; 18% for tickets > Rs.100 base price
- **Config properties:** gst.rate.high=18.0, gst.rate.low=12.0, gst.threshold=100.0
- **GST is always non-refundable** on cancellation, as per Indian tax regulation

### Notification System (Simulated)
- Email / SMS / Push observers log to System.out (production would wire JavaMailSender / Twilio)
- Notifications triggered on: BOOKING_CONFIRMED, BOOKING_CANCELLED, TICKET_ISSUED

---

## OOAD Design Patterns

### 1. Singleton Pattern -- MongoDBSingleton

**File:** patterns/MongoDBSingleton.java

**Implementation verified:** Private static instance, private constructor, thread-safe getInstance(String uri, String dbName) using double-checked locking with synchronized. Spring @Component also ensures Spring IoC treats this as a singleton bean. The static instance field and synchronized block are present in the source.

### 2. Factory Pattern -- TicketFactory

**Files:** patterns/TicketFactory.java, AbstractTicket.java, RegularTicket.java, PremiumTicket.java, VIPTicket.java

**Implementation verified:** createTicket(seatType, ...) uses a Java switch expression to instantiate RegularTicket / PremiumTicket / VIPTicket. An overloaded variant accepts a List of seat numbers and calls determineSeatType() which maps rows A-B to VIP, C-E to PREMIUM, F-J to REGULAR. Ticket codes are prefixed VIP-, PRE-, REG-. AbstractTicket defines abstract calculatePrice(double) and getTicketCategory() which all subtypes must implement (LSP).

### 3. Strategy Pattern -- PaymentStrategy + PaymentContext

**Files:** patterns/PaymentStrategy.java (interface), UPIPaymentStrategy.java, CardPaymentStrategy.java, WalletPaymentStrategy.java, PaymentContext.java

**Implementation verified:** PaymentStrategy interface declares pay(userId, amount, details), getMethodName(), and validate(details). PaymentContext receives all PaymentStrategy Spring beans via constructor injection (List<PaymentStrategy>) and resolves the correct strategy at runtime by matching method name. After the strategy executes, PaymentContext calls PaymentGatewayAdapter to verify via the external API.

### 4. Adapter Pattern -- PaymentGatewayAdapter

**Files:** patterns/PaymentGatewayAdapter.java (adapter), PaymentGateway.java (target interface), ExternalPaymentAPI.java (adaptee)

**Implementation verified:** ExternalPaymentAPI.initiateTransaction(merchantId, customerId, amount, mode) is incompatible with our PaymentGateway.processPayment(userId, amount, method). The adapter translates: UPI to NET_BANKING, CARD to CREDIT, WALLET to EWALLET. Response TXN_SUCCESS_ prefix is stripped to extract the transaction ID.

### 5. Observer Pattern -- BookingNotificationSubject + BookingEventPublisher

**Files:** patterns/BookingNotificationSubject.java, BookingEventPublisher.java, BookingConfirmedEvent.java, EmailNotificationListener.java, SMSNotificationListener.java

**Implementation verified:** Two complementary implementations co-exist:
- **Manual Observer** (BookingNotificationSubject): Maintains List<BookingObserver>. At construction, registers EmailNotificationObserver, SMSNotificationObserver, and PushNotificationObserver. Broadcasts notifyBookingConfirmed, notifyBookingCancelled, notifyTicketIssued to all.
- **Spring Event Bus** (BookingEventPublisher): Wraps Spring's ApplicationEventPublisher to publish BookingConfirmedEvent. @EventListener classes receive it asynchronously (enabled by @EnableAsync on the main class). This is the lower-coupling production approach.

---

## Getting Started

### Prerequisites

| Software | Version | Notes |
|---|---|---|
| Java JDK | 17+ | Required by pom.xml (java.version=17) |
| MongoDB | 6.x+ | Must be running locally on port 27017 |
| Node.js | 18+ | For the React frontend |
| Maven | (uses wrapper) | mvnw / mvnw.cmd included -- no separate install needed |

### Step 1 -- Configure application.properties

All configuration is in src/main/resources/application.properties. Actual property names (copy as-is):

```properties
# MongoDB (required)
spring.data.mongodb.uri=mongodb://localhost:27017/moviebooking
spring.data.mongodb.database=moviebooking

# Server
server.port=8080

# JWT -- change secret in production
jwt.secret=moviebooking_super_secret_key_ooad_project_2024_secure
jwt.expiration=86400000

# TMDB API -- optional, falls back to curated movie list if absent
tmdb.api.key=YOUR_TMDB_API_KEY_HERE
tmdb.api.base-url=https://api.themoviedb.org/3
tmdb.image.base-url=https://image.tmdb.org/t/p/w500

# GST
gst.rate.high=18.0
gst.rate.low=12.0
gst.threshold=100.0
```

> There are no .env files in this project. All config lives in application.properties. No environment variable overrides are wired in the current codebase.

> **Security warning:** The TMDB API key committed in application.properties is a real key. Rotate it before making this repository public.

### Step 2 -- Start MongoDB

```bash
# Windows service
net start MongoDB

# Or manually
mongod --dbpath C:\data\db
```

### Step 3 -- Start the Backend

```bash
# Windows PowerShell (from project root)
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Backend starts at http://localhost:8080. DataLoader auto-seeds all data on first run.

### Step 4 -- Start the Frontend

```bash
cd frontend
npm install
npm run dev
# Starts at http://localhost:5173 (Vite default)
```

### Default Credentials

| Role | Email | Password |
|---|---|---|
| Admin | admin@cinebook.com | password123 |
| User | durga@example.com | password123 |

> **Important:** Always register users through POST /api/auth/register rather than inserting raw documents with the mongodb_seed.js hashes, which are example values only.

---

## Project Structure

```
CineBook_MovieBooking_Project/
├── src/                           # Spring Boot backend
│   └── main/
│       ├── java/com/project/moviebooking/
│       │   ├── MoviebookingApplication.java
│       │   ├── config/            # Security, JWT, DataLoader
│       │   ├── controller/        # 8 REST controllers
│       │   ├── dto/               # Request/response DTOs
│       │   ├── exception/         # Global exception handling
│       │   ├── model/             # 9 MongoDB document models
│       │   ├── patterns/          # 19 OOAD pattern classes
│       │   ├── repository/        # Spring Data MongoDB interfaces
│       │   └── service/           # Business logic + ShowRefreshService
│       └── resources/
│           └── application.properties
├── frontend/                      # React + Vite frontend
│   ├── src/
│   │   ├── api/axios.js           # Axios instance with JWT interceptor and proxy
│   │   ├── components/            # Navbar, ProtectedRoute, Toast, CancelBookingModal
│   │   ├── pages/                 # 11 page components
│   │   ├── App.jsx                # Router + all route definitions
│   │   └── main.jsx               # React DOM entry point
│   └── package.json
├── mongodb_seed.js                # Reference seed script (DB auto-seeds on startup)
├── cleanup_shows.js               # Utility: remove orphaned shows from DB
├── fix_shows.js                   # Utility: repair corrupt show documents
├── drop_data.js                   # Utility: wipe all collections (DESTRUCTIVE)
├── pom.xml                        # Maven build descriptor
└── mvnw / mvnw.cmd                # Maven wrapper scripts
```

---

## Known Issues and Notes

- **GET /api/payments/my** -- the current implementation always calls getPaymentByBookingId("ALL") which is not a valid booking ID. This endpoint does not return the current user's payments correctly. This is a known incomplete implementation in PaymentController.java.
- **GET /api/tickets/admin/all** -- returns null data in the response body. The ticket list is not wired; only a booking count string is returned in the message field.
- **Notifications are simulated** -- Email, SMS, and Push notifications print to System.out. Wire JavaMailSender (Email) or Twilio (SMS) in the listener classes for production.
- **No environment variable support** -- jwt.secret and tmdb.api.key are plain text in application.properties. Externalize via Spring's ${ENV_VAR:default} syntax or Spring Cloud Config for production.

# Complete Technical Architecture Guide
**Movie Ticket Booking System (LLD)**

---

## 1. Executive Summary
This document serves as the master guide to the Low-Level Design (LLD) of the Movie Ticket Booking Engine. It breaks down the entire lifecycle of a ticket booking operation—from how cinemas and screens are modeled, to how shows are instantiated without overlapping seat histories, to the hardcore concurrency locks preventing double-bookings. It also transparently addresses the bounds of this project (what is intentionally lacking, such as admin scheduling overlap prevention).

---

## 2. Core Domain Models & Entity Relationships

The system is built on highly decoupled Java objects to simulate a real-world theatre environment.

### 2.1 The Infrastructure Models
* **[Theatre](file:///e:/Projects/MovieTicketBookingSystem/src/model/Theatre.java#3-20)**: The physical cinema building (e.g., "PVR Cinemas, Mall of America"). Holds an ID and a Name.
* **[Screen](file:///e:/Projects/MovieTicketBookingSystem/src/model/Screen.java#3-20)**: An individual auditorium within the Theatre. Belongs to exactly 1 [Theatre](file:///e:/Projects/MovieTicketBookingSystem/src/model/Theatre.java#3-20).

### 2.2 The Scheduling Models (The Event)
* **[Show](file:///e:/Projects/MovieTicketBookingSystem/src/model/Show.java#7-49)**: This is the most crucial class in the architecture. A [Show](file:///e:/Projects/MovieTicketBookingSystem/src/model/Show.java#7-49) represents a specific Movie airing on a specific [Screen](file:///e:/Projects/MovieTicketBookingSystem/src/model/Screen.java#3-20) at a specific `LocalDateTime` (e.g., "Avengers", Screen 1, 10:00 AM).
  * **Critical Architecture Rule (No Seat Resetting)**: A [Show](file:///e:/Projects/MovieTicketBookingSystem/src/model/Show.java#7-49) holds its own internal, thread-safe `ConcurrentHashMap` of [Seat](file:///e:/Projects/MovieTicketBookingSystem/src/model/Seat.java#3-30) objects. When a movie finishes, the system **does not** "reset" the seats from BOOKED to AVAILABLE. Doing so would destroy the historical booking data. Instead, when the 1:00 PM movie begins, the system generates a **completely new [Show](file:///e:/Projects/MovieTicketBookingSystem/src/model/Show.java#7-49) object** loaded with a brand new map of clean [Seat](file:///e:/Projects/MovieTicketBookingSystem/src/model/Seat.java#3-30) objects. 
  * This guarantees 10:00 AM bookings never interfere with 1:00 PM bookings, and historical data remains immutable.

### 2.3 The transactional Models
* **[Seat](file:///e:/Projects/MovieTicketBookingSystem/src/model/Seat.java#3-30)**: An individual chair inside the [Show](file:///e:/Projects/MovieTicketBookingSystem/src/model/Show.java#7-49) map. It holds an Enum `SeatType` (VIP, REGULAR) and an Enum `SeatStatus` (AVAILABLE, LOCKED, BOOKED).
* **[SeatLock](file:///e:/Projects/MovieTicketBookingSystem/src/model/SeatLock.java#5-36)**: A temporary security object linking a [Seat](file:///e:/Projects/MovieTicketBookingSystem/src/model/Seat.java#3-30) to a [User](file:///e:/Projects/MovieTicketBookingSystem/src/model/Booking.java#24-27) for a fixed duration of time (e.g., 5 minutes) via `Instant` timestamps.
* **[Booking](file:///e:/Projects/MovieTicketBookingSystem/src/model/Booking.java#5-40)**: The final receipt object permanently binding the User to their [Seats](file:///e:/Projects/MovieTicketBookingSystem/src/model/Booking.java#32-35) with the final financial total attached.

---

## 3. The Core Booking Mechanisms

### 3.1 Resolving Concurrency (Double-Booking Prevention)
The massive challenge of ticketing is stopping two users from clicking the identical seat at the identical millisecond. 
* **The Solution**: We utilize **Seat-Level Monitor Locking** (`synchronized (seat)`).
* **How it works**: By synchronizing against the target `seat` memory object instance, if User A and User B concurrently target Seat `A1`, the JVM forces them into a single-file line physically. User A checks the status, flips it to `LOCKED`, and finishes. User B then executes, checks the status, sees it is now `LOCKED`, and receives a clean [SeatLockException](file:///e:/Projects/MovieTicketBookingSystem/src/exception/SeatLockException.java#3-8) rejection.
* **Why it scales**: Because the lock is bound to `A1`, User C can book `A2` simultaneously with absolutely zero CPU interference.

### 3.2 Automated Time-Based Lock Sweeping
When a lock is acquired, the user must pay. If their payment fails or they leave the webpage, the lock must expire.
* **The Solution**: A `ScheduledExecutorService` (Background Daemon Thread).
* **How it works**: A background thread spins continuously every 1 second in the [SeatLockService](file:///e:/Projects/MovieTicketBookingSystem/src/service/SeatLockService.java#16-105). It quietly traverses all active lock caches. If it finds a [SeatLock](file:///e:/Projects/MovieTicketBookingSystem/src/model/SeatLock.java#5-36) where the `expirationTime` has passed the `Instant.now()`, the daemon forcefully strips the lock and reverts the actual [Seat](file:///e:/Projects/MovieTicketBookingSystem/src/model/Seat.java#3-30) back to `AVAILABLE`.

### 3.3 Dynamic Pricing Calculations
* **The Solution**: The Strategy Design Pattern ([PricingStrategy](file:///e:/Projects/MovieTicketBookingSystem/src/strategy/PricingStrategy.java#6-9)).
* **How it works**: Pricing isn't hardcoded. Depending on the day or event, the system injects a strategy (like [WeekendPricingStrategy](file:///e:/Projects/MovieTicketBookingSystem/src/strategy/WeekendPricingStrategy.java#6-21) or [RegularPricingStrategy](file:///e:/Projects/MovieTicketBookingSystem/src/strategy/RegularPricingStrategy.java#6-21)) which calculates the final fee based on the individual `SeatType` variations. This obeys the **Open-Closed Principle**, allowing future strategies (e.g. `MatineeDiscountStrategy`) to be added without touching core business logic.

---

## 4. Boundary Limits & "What is Lacking"
This project focuses intensely on the **Booking & Concurrency Engine**. Therefore, certain administrative boundaries were intentionally omitted. If adapting this to a full-stack, real-world application, the following would be required:

### 4.1 Missing: Screen Scheduling Overlap Prevention
* **Current State**: The system accurately ties a [Show](file:///e:/Projects/MovieTicketBookingSystem/src/model/Show.java#7-49) to a [Screen](file:///e:/Projects/MovieTicketBookingSystem/src/model/Screen.java#3-20). However, there is no validation preventing an Administrator from scheduling two different [Show](file:///e:/Projects/MovieTicketBookingSystem/src/model/Show.java#7-49) objects on the identical [Screen](file:///e:/Projects/MovieTicketBookingSystem/src/model/Screen.java#3-20) at the identical time. 
* **Required Fix**: If building the `TheatreAdminService`, whenever a new [Show](file:///e:/Projects/MovieTicketBookingSystem/src/model/Show.java#7-49) is instantiated, the system must query the [Screen](file:///e:/Projects/MovieTicketBookingSystem/src/model/Screen.java#3-20)'s existing list of Shows. It must check if `newShow.startTime` occurs before [(existingShow.startTime + movieDuration)](file:///e:/Projects/MovieTicketBookingSystem/src/model/Show.java#7-49). If an overlap exists, it should throw a `ScheduleConflictException`.

### 4.2 Missing: Persistent Database State
* **Current State**: All mapping, locking, and seating is tracked natively in standard JVM memory (RAM). When the Java program terminates, the data is lost.
* **Required Fix**: Move state mutations (like `seat.setStatus(BOOKED)`) into transactional **ACID Relational Database** queries (PostgreSQL / MySQL) utilizing row-level locks (`SELECT ... FOR UPDATE`).

### 4.3 Missing: Distributed Locks
* **Current State**: `synchronized (seat)` works flawlessly for a single local server running Java.
* **Required Fix**: If scaling to a massive server cluster (e.g., 10 API instances running simultaneously), a local synchronized block won't stop Server 1 from booking the same seat as Server 2. You must implement a **Distributed Caching Lock** layer, utilizing technology such as **Redis** (via Redisson) to lock unique String identifiers (e.g., `LOCK_SHOW1_SEAT_A1`) globally across the cloud.

---

## 5. Master API Execution Flow (The User Journey)

1. **User Request**: HTTP Payload requests [bookSeats()](file:///e:/Projects/MovieTicketBookingSystem/src/service/BookingService.java#25-74) containing Show ID `SHOW1` and designated Seat IDs `["A1", "A2"]`.
2. **Entity Validation**: System queries `SHOW1` and extracts references to the real memory [Seat](file:///e:/Projects/MovieTicketBookingSystem/src/model/Seat.java#3-30) objects. It verifies the seats physically exist.
3. **Lock Acquisition Request**: [BookingService](file:///e:/Projects/MovieTicketBookingSystem/src/service/BookingService.java#16-75) delegates to [SeatLockService](file:///e:/Projects/MovieTicketBookingSystem/src/service/SeatLockService.java#16-105).
4. **Transaction Attempt**: [SeatLockService](file:///e:/Projects/MovieTicketBookingSystem/src/service/SeatLockService.java#16-105) seizes the intrinsic thread monitor for `Seat A1`. It verifies the state is `AVAILABLE`. The state structurally mutates to `LOCKED`. An expiring timer is assigned to it inside the cache map.
5. **Simulated Payment Latency**: System thread sleeps momentarily, simulating external payment gateway routing.
6. **Confirmation Verification**: Before locking the receipt, [BookingService](file:///e:/Projects/MovieTicketBookingSystem/src/service/BookingService.java#16-75) runs [validateLock()](file:///e:/Projects/MovieTicketBookingSystem/src/service/SeatLockService.java#73-81) against the cache. This ensures the background Daemon Sweeper thread did not strip the lock due to a timeout while the user was traversing the payment gateway.
7. **Finalization Makeover**: If valid, [confirmLock()](file:///e:/Projects/MovieTicketBookingSystem/src/service/SeatLockService.java#52-60) permanently deletes the temporary lock mappings. The [Seat](file:///e:/Projects/MovieTicketBookingSystem/src/model/Seat.java#3-30) physically mutates into `BOOKED`. [PricingStrategy](file:///e:/Projects/MovieTicketBookingSystem/src/strategy/PricingStrategy.java#6-9) totals the amount. A [Booking](file:///e:/Projects/MovieTicketBookingSystem/src/model/Booking.java#5-40) receipt object is formulated and returned to the client controller.

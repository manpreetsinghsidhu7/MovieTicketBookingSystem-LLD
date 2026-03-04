# 🎬 Movie Ticket Booking System - Technical Report

## 1. Overview
The Movie Ticket Booking System is a scalable, object-oriented, console-based Java application designed to handle theater bookings. It focuses heavily on **Low-Level Design (LLD)** mechanics, including state management, concurrent processing, timer-based seat locking, and extensible pricing strategies.

## 2. Core Domain Model
The domain is structured into clear, decoupled entities reflecting a real-world theater system. All models are grouped in `src/model/`.
* **Theatre**: Represents the physical cinema location (e.g., PVR Cinemas).
* **Screen**: Represents a specific hall/auditorium within a Theatre.
* **Show**: Represents a specific movie airing at a specific Screen at a set `StartTime`. Each show holds a **Thread-Safe Map** `ConcurrentHashMap<String, Seat>` storing its distinct seat layout. By tying the seat layout map directly to the `Show`, different shows do not interfere with one another's bookings.
* **Seat**: Represents an actual chair. Contains inherent constraints such as `SeatType` (VIP, REGULAR, PREMIUM) and `SeatStatus`.
* **SeatLock**: A temporary associative mapping holding the `Seat`, `Show`, `UserId`, and a strict time expiration (`Instant.now().plusSeconds()`).
* **Booking**: The finalized transaction object holding confirmed `Seats` and the total calculated `amountPaid` for the user.

## 3. Core Mechanics & Engineering Features

### 3.1. Seat State Management
A seat relies on an Enum `SeatStatus` for strict behavioral states to prevent illegal mutations.
The flow operates linearly:
`AVAILABLE` ➔ `LOCKED` ➔ `BOOKED`

Direct transitions from `AVAILABLE` to `BOOKED` are strictly impossible. A user **must** acquire a lock first through the `SeatLockService`. If a lock expires without payment, it rolls back gracefully:
`LOCKED` ➔ `AVAILABLE`

### 3.2. Seat Locking & Concurrency Prevention (Critical Component)
In highly concurrent ticketing systems (like BookMyShow), preventing Double-Booking (a Race Condition) is the biggest priority.
- **Seat-Level Granular Locking**: Instead of using global locks or locking an entire `Show` (which would bottleneck system performance massively), the thread synchronization is localized down to the specific `Seat` instance being accessed.
```java
synchronized (seat) {
    if (seat.getStatus() == SeatStatus.AVAILABLE) {
        seat.setStatus(SeatStatus.LOCKED);
        // ...save lock in map
    } else {
        throw new SeatLockException();
    }
}
```
This isolates the transaction. If 1,000 users are booking 1,000 different seats, the threads execute in complete parallel width. However, if 10 users hit the exact same seat concurrently (`Seat A1`), the JVM `synchronized` monitor forces them into a queue. The winning thread changes the state to `LOCKED`; the subsequent 9 threads fail instantly without crashing the application.

### 3.3. Time-Based Lock Expiration (Background Sweeping)
If a user acquires a lock but closes their browser, or their payment pipeline fails, the seat cannot remain `LOCKED` permanently. 
We avoided "lazy eviction" (Option A: checking expiration manually only when the next person requests the exact same seat). Instead, we built **Option B: Background Threads**:
- `SeatLockService` utilizes a `ScheduledExecutorService` running cleanly on a secondary background thread.
- It iterates across all active locks traversing exact timestamps automatically. 
- If `lock.isExpired()` returns **true**, that seat is forcefully flushed by the system and restored to `AVAILABLE`.

### 3.4. Dynamic Pricing Pipeline (Strategy Pattern)
Pricing variations based on seats, locations, surges, holidays, or weekends are handled gracefully using the **Strategy Design Pattern**.
- `PricingStrategy.java` interface acts as the universal blueprint.
- Implementations like `RegularPricingStrategy` and `WeekendPricingStrategy` hold the isolated calculation logic targeting things like `SeatType` (VIP vs. REGULAR).
- Business layer code simply invokes `calculatePrice()` without knowing *how* it calculates it. We can slot in a `FestivalPricingStrategy` in the future without modifying existing core classes, firmly preserving the **Open-Closed Principle** (SOLID).

## 4. normal Booking Flow (Step-by-Step)
1. **User Request**: The User selects an array of Seat IDs for a specific Show.
2. **Initial Validation**: `BookingService` verifies those seats actually exist on the `Show` object's internal Layout.
3. **Lock Acquisition**: `BookingService` signals the `SeatLockService` to bind the selected seats to the `userId`. The system checks `SeatStatus` via the object `synchronized` block and commits the timer.
4. **Latency simulation**: System thread pauses via `Thread.sleep` acting as the network round-trip for user payment confirmation.
5. **Confirmation phase**: Before making the ultimate commit to `BOOKED`, the system redundantly double-checks the lock to ensure it hasn't timed-out in the background during the payment phase. If the `validateLock()` is successful, the Seat transitions to `BOOKED` permanently, money is calculated via `PricingStrategy`, and the temporary trace is cleared natively by `lockService.confirmLock()`.

## 5. Summary of Design Patterns Leveraged
* **Strategy Pattern**: Driving variations in movie ticket pricing dynamically.
* **State Behavior**: Managed strictly via Enum (`SeatStatus` state machine limits).
* **Dependency Injection**: Injecting `SeatLockService` and `PricingStrategy` centrally into `BookingService` cleanly via the Main thread rather than hiding tight couplings inside the service constructors.

## 6. Real-World Equivalents (For Interview Discussions)
If asked how you translate this exact local-LLD blueprint into a distributed Production architecture:
1. **Concurrency Control:** Replace the local `synchronized(seat)` implementation and `ConcurrentHashMap` with **Distributed Caching (Redis)**. You would rely on Redis mechanisms like `Redisson` lock or simple `SETNX` commands to prevent multi-server race conditions.
2. **Lock Expiration:** Rip out the local `ScheduledExecutorService` Daemon. Instead, assign a **Time-To-Live (TTL)** directly against the Redis Key during the `SET` operation, letting Redis autistically delete the lock data from memory itself.
3. **Data Integrity:** Replace local Java assignments (`seat.setStatus()`) with strict **Relational Database** commands (MySQL/PostgreSQL) enforcing Isolation levels, ensuring operations reflect across all API nodes.

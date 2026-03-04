<div align="center">
  <h1>🎬 Movie Ticket Booking System (LLD)</h1>
  <p>A scalable, thread-safe, core Java implementation of a modern movie ticket booking engine.</p>
</div>

<br />

## 📌 Overview
This project is a console-based **Low-Level Design (LLD)** implementation of a Movie Ticket Booking System (similar to BookMyShow). Built purely in **Core Java** without relying on external frameworks like Spring or actual databases, it focuses heavily on object-oriented programming (OOP), memory management, multithreading, and scalable concurrency.

## 🚀 Core Features
*   **Seat Locking Mechanism**: Temporarily holds seats for a user during the payment pipeline.
*   **Time-Based Expiration**: Actively runs a background scheduled daemon to automatically flush expired locks back to the community pool.
*   **Concurrency & Thread Safety**: Solves the "Double Booking" problem. Utilizes fine-grained object-level monitor locks (`synchronized (seat)`) ensuring maximum parallel throughput for independent bookings while safely queuing collisions.
*   **Dynamic Pricing Rules**: Leverages the Strategy Design Pattern to scale pricing variations (e.g., Weekend Surges, VIP seating) seamlessly.

## 🏗️ Architecture & Domain Entities
The domain model is designed to be highly decoupled:
*   **Theatre**: The physical cinema establishment.
*   **Screen**: An auditorium inside the Theatre.
*   **Show**: A specific screening of a movie. Contains a dedicated, thread-safe `ConcurrentHashMap` of `.Seats()` to prevent booking interferences across different shows.
*   **Seat**: An individual chair, holding a fixed `SeatType` and dynamic `SeatStatus`.
*   **SeatLock**: A temporal mapping binding a `Seat`, a `Show`, a `userId`, and strict `Instant` TTL timestamps.
*   **Booking**: The final immutable receipt generated upon successful transaction.

## ⚙️ Advanced Engineering Mechanics

### 1. Granular Concurrency Control
If 1,000 users try booking seats at the same time, using a global or method-level lock would force them into a single-file line—destroying system performance entirely. 
Instead, we synchronize directly on the **target memory object** itself:
```java
synchronized (seat) {
    if (seat.getStatus() == SeatStatus.AVAILABLE) {
        seat.setStatus(SeatStatus.LOCKED);
        // Map to lock cache...
    }
}
```
**Outcome**: If User A and User B concurrently target Seat `A1`, they queue up at the memory level. User A wins, User B gets an instant `SeatLockException` cleanly rejecting the attempt. If User C targets Seat `A2` simultaneously, they proceed perfectly in parallel alongside A and B.

### 2. Automated Lock Expiration Sweeper
We do not use lazy evaluation for lock expiration (which risks stale data). Instead, `SeatLockService` spins up a dedicated `ScheduledExecutorService` running completely in the background:
*   Iterates actively across all internal lock maps asynchronously.
*   If `Instant.now().isAfter(expirationTime)`, it automatically strips the lock and restores the `Seat` to `AVAILABLE`.

### 3. State & Strategy Patterns
*   **State Integrity**: A seat's flow is strictly limited to an Enum (`AVAILABLE` ➔ `LOCKED` ➔ `BOOKED`). Bypassing `LOCKED` directly to `BOOKED` is structurally impossible.
*   **Pricing Strategy**: The `PricingStrategy` interface prevents monolithic `switch-case` architectures. Injecting a `WeekendPricingStrategy` alters the cost dynamically based on `SeatType` without requiring modifications to the main `BookingService` source code (Open-Closed Principle).

## 🔄 End-to-End API Booking Flow
1.  **Request**: User invokes `bookSeats()` with a batch of `Seat IDs`.
2.  **Acquire Lock**: System individually probes each `Seat`. Subject to concurrency monitors, it successfully flips the State to `LOCKED` if empty.
3.  **Payment Phase**: Simulates a network transaction delay for financial routing.
4.  **Verification**: Before finalizing, the system double checks that the lock hasn't been swept away by the background timeout daemon. 
5.  **Commit**: If validated, locks are cleanly destroyed from memory and the State makes its final mutation to `BOOKED`.

## 💻 Local Testing & Execution

### Prerequisites
*   Java Development Kit (JDK 8 or higher)

### Instructions
1.  Clone this repository and open your terminal.
2.  Navigate directly to the `src` directory:
    ```bash
    cd src
    ```
3.  Compile the project into a new `out` output directory:
    ```bash
    javac -d out exception/*.java model/*.java strategy/*.java service/*.java Main.java
    ```
    *(Note for Windows command prompt formatting use: `javac -d out exception\*.java model\*.java strategy\*.java service\*.java Main.java`)*
4.  Execute the main simulation:
    ```bash
    java -cp out Main
    ```

## 🧪 Simulation Output Example
The `Main.java` entry point includes built-in edge-case simulations, deploying an `ExecutorService` thread pool to launch 10 identical parallel threads attacking Seat `A1` millisecond-for-millisecond to prove Concurrency safety:

```text
--- Starting Movie Ticket Booking System Simulation ---

10 users trying to book Seat A1 at the exact same time.
User [User-3] successfully locked seats: [A1]
User [User-9] Exception: Seat A1 is already LOCKED
User [User-10] Exception: Seat A1 is already LOCKED
User [User-8] Exception: Seat A1 is already LOCKED
User [User-2] Exception: Seat A1 is already LOCKED
User [User-7] Exception: Seat A1 is already LOCKED
User [User-6] Exception: Seat A1 is already LOCKED
User [User-1] Exception: Seat A1 is already LOCKED
User [User-4] Exception: Seat A1 is already LOCKED
User [User-5] Exception: Seat A1 is already LOCKED
Booking Confirmed! ✅ ID: 909ad4f0-22dc-4b9f-85f7-2501b641b037 | By: User-3 | Total: Rs.250.0

Seat A1 Final Status: BOOKED

User-X trying to lock Seat A2, but won't book it.
Seat A2 Status after lock attempt: LOCKED
Waiting 12 seconds for lock to expire automatically...
[System]: Lock expired and released for seat: A2 at Show SHOW1
Seat A2 Status after wait: AVAILABLE

--- Simulation complete ---
```

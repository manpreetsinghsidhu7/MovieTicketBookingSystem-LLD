<div align="center">
  <h1>🎬 Movie Ticket Booking System (LLD)</h1>
  <p>A scalable, thread-safe, core Java implementation of a modern movie ticket booking engine (BookMyShow clone).</p>
</div>

<br />

## 📌 Overview
This project is a console-based **Low-Level Design (LLD)** implementation of a Movie Ticket Booking System. Built purely in **Core Java** without relying on external frameworks like Spring or actual databases, it focuses heavily on object-oriented programming (OOP), memory management, multithreading, and scalable concurrency.

If you are a developer, recruiter, or student reviewing this repository to understand the core engine mechanics, **please read the comprehensive master guide**: 
👉 **[Master Guide: Complete Architecture & UML Details](movie_ticket_system_complete_guide.md)** 👈

The Master Guide contains the complete breakdown of the system, including:
* UML Class & State Diagrams
* API Sequence Flows
* Deep-dives into the Concurrency (`synchronized (seat)`) logic
* The "Background Sweeper" daemon mechanics 

---

## 🚀 Essential Features
*   **Seat Locking Mechanism**: Temporarily holds seats for a user during the payment pipeline.
*   **Time-Based Expiration**: Actively runs a background scheduled daemon to automatically flush expired locks back to the community pool.
*   **Concurrency & Thread Safety**: Solves the "Double Booking" problem using fine-grained object-level monitor locks ensuring maximum parallel throughput for independent bookings while safely queuing collisions.
*   **Dynamic Pricing Rules**: Leverages the Strategy Design Pattern to scale pricing variations (e.g., Weekend Surges) seamlessly.
*   **Strict Entity Boundaries**: Seats never shift directly from `AVAILABLE` to `BOOKED`. The state machine forces all transactions through a secure `LOCKED` pipeline.

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

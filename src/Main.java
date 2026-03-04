import exception.SeatLockException;
import model.Screen;
import model.Seat;
import model.SeatType;
import model.Show;
import model.Theatre;
import service.BookingService;
import service.SeatLockService;
import strategy.RegularPricingStrategy;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Starting Movie Ticket Booking System Simulation ---\n");

        Theatre theatre = new Theatre("T1", "PVR Cinemas");
        Screen screen = new Screen("S1", theatre);
        Show show = new Show("SHOW1", "MOV_AVENGERS", screen, LocalDateTime.now().plusDays(1));

        show.addSeat(new Seat("A1", SeatType.VIP));
        show.addSeat(new Seat("A2", SeatType.REGULAR));

        SeatLockService lockService = new SeatLockService();
        RegularPricingStrategy pricingStrategy = new RegularPricingStrategy();
        BookingService bookingService = new BookingService(lockService, pricingStrategy);

        System.out.println("10 users trying to book Seat A1 at the exact same time.");
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 1; i <= 10; i++) {
            final String userId = "User-" + i;
            executor.submit(() -> {
                try {
                    bookingService.bookSeats(show, Collections.singletonList("A1"), userId);
                } catch (SeatLockException e) {
                    System.err.println("User [" + userId + "] Exception: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("User [" + userId + "] Unknown Error: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("\nSeat A1 Final Status: " + show.getSeat("A1").getStatus() + "\n");

        System.out.println("User-X trying to lock Seat A2, but won't book it.");
        try {
            lockService.lockSeats(show, Collections.singletonList(show.getSeat("A2")), "User-X");
            System.out.println("Seat A2 Status after lock attempt: " + show.getSeat("A2").getStatus());

            // Wait for lock to expire
            System.out.println("Waiting 12 seconds for lock to expire automatically...");
            Thread.sleep(12000);
            System.out.println("Seat A2 Status after wait: " + show.getSeat("A2").getStatus());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        lockService.shutdown();
        System.out.println("\n--- Simulation complete ---");
    }
}

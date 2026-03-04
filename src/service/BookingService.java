package service;

import exception.InvalidSeatException;

import exception.SeatLockException;
import model.Booking;
import model.Seat;
import model.SeatStatus;
import model.Show;
import strategy.PricingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BookingService {
    private final SeatLockService seatLockService;
    private final PricingStrategy pricingStrategy;

    public BookingService(SeatLockService seatLockService, PricingStrategy pricingStrategy) {
        this.seatLockService = seatLockService;
        this.pricingStrategy = pricingStrategy;
    }

    public Booking bookSeats(Show show, List<String> seatIds, String userId) {
        List<Seat> seatsToBook = new ArrayList<>();

        // validate seats
        for (String seatId : seatIds) {
            Seat seat = show.getSeat(seatId);
            if (seat == null) {
                throw new InvalidSeatException("Seat not found: " + seatId);
            }
            seatsToBook.add(seat);
        }

        // lock the seats
        seatLockService.lockSeats(show, seatsToBook, userId);
        System.out.println("User [" + userId + "] successfully locked seats: " + seatIds);

        // simulate payment delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // confirm booking
        double totalAmount = 0.0;
        List<Seat> bookedSeats = new ArrayList<>();

        for (Seat seat : seatsToBook) {
            synchronized (seat) {
                if (!seatLockService.validateLock(show, seat, userId)) {
                    System.err.println("User [" + userId + "] lock invalid or expired for seat " + seat.getId()
                            + ". Rolling back.");
                    // rollback missing lock
                    seatLockService.unlockSeats(show, seatsToBook, userId);
                    throw new SeatLockException("Lock expired for seat: " + seat.getId() + ". Booking failed!");
                }

                seatLockService.confirmLock(show, seat, userId);
                seat.setStatus(SeatStatus.BOOKED);
                totalAmount += pricingStrategy.calculatePrice(seat, show);
                bookedSeats.add(seat);
            }
        }

        Booking booking = new Booking(UUID.randomUUID().toString(), userId, show, bookedSeats, totalAmount);
        System.out.println(
                "Booking Confirmed! ✅ ID: " + booking.getId() + " | By: " + userId + " | Total: Rs." + totalAmount);
        return booking;
    }
}

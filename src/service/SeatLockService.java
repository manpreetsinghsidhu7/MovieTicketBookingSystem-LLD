package service;

import model.Seat;
import model.SeatLock;
import model.SeatStatus;
import model.Show;
import exception.SeatLockException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SeatLockService {
    private final Map<String, Map<String, SeatLock>> locks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SeatLockService() {
        scheduler.scheduleAtFixedRate(this::cleanExpiredLocks, 1, 1, TimeUnit.SECONDS);
    }

    public void lockSeats(Show show, List<Seat> seats, String userId) {
        seats.forEach(seat -> lockSeat(show, seat, userId, 10));
    }

    private void lockSeat(Show show, Seat seat, String userId, int timeoutInSeconds) {
        synchronized (seat) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new SeatLockException("Seat " + seat.getId() + " is already " + seat.getStatus());
            }
            seat.setStatus(SeatStatus.LOCKED);
            locks.computeIfAbsent(show.getId(), k -> new ConcurrentHashMap<>())
                    .put(seat.getId(), new SeatLock(seat, show, userId, timeoutInSeconds));
        }
    }

    public void unlockSeats(Show show, List<Seat> seats, String userId) {
        seats.stream().filter(seat -> validateLock(show, seat, userId)).forEach(seat -> unlockSeat(show, seat));
    }

    public void confirmLock(Show show, Seat seat, String userId) {
        Map<String, SeatLock> showLocks = locks.get(show.getId());
        if (showLocks != null)
            showLocks.remove(seat.getId());
    }

    private void unlockSeat(Show show, Seat seat) {
        synchronized (seat) {
            if (seat.getStatus() == SeatStatus.LOCKED) {
                seat.setStatus(SeatStatus.AVAILABLE);
            }
            confirmLock(show, seat, null);
        }
    }

    public boolean validateLock(Show show, Seat seat, String userId) {
        Map<String, SeatLock> showLocks = locks.get(show.getId());
        if (showLocks == null)
            return false;

        SeatLock lock = showLocks.get(seat.getId());
        return lock != null && lock.getUserId().equals(userId) && !lock.isExpired();
    }

    private void cleanExpiredLocks() {
        try {
            locks.values()
                    .forEach(showLocks -> showLocks.values().stream().filter(SeatLock::isExpired).forEach(lock -> {
                        unlockSeat(lock.getShow(), lock.getSeat());
                        System.out.println("[System]: Lock expired and released for seat: " + lock.getSeat().getId());
                    }));
        } catch (Exception ignored) {
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}

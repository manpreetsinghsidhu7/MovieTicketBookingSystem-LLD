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
    // ShowId -> (SeatId -> SeatLock)
    private final Map<String, Map<String, SeatLock>> locks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SeatLockService() {
        // start cleanup scheduler
        scheduler.scheduleAtFixedRate(this::cleanExpiredLocks, 1, 1, TimeUnit.SECONDS);
    }

    public void lockSeats(Show show, List<Seat> seats, String userId) {
        for (Seat seat : seats) {
            lockSeat(show, seat, userId, 10);
        }
    }

    private void lockSeat(Show show, Seat seat, String userId, int timeoutInSeconds) {
        synchronized (seat) {
            if (seat.getStatus() == SeatStatus.AVAILABLE) {
                seat.setStatus(SeatStatus.LOCKED);
                SeatLock lock = new SeatLock(seat, show, userId, timeoutInSeconds);
                locks.computeIfAbsent(show.getId(), k -> new ConcurrentHashMap<>()).put(seat.getId(), lock);
            } else {
                throw new SeatLockException("Seat " + seat.getId() + " is already " + seat.getStatus());
            }
        }
    }

    public void unlockSeats(Show show, List<Seat> seats, String userId) {
        for (Seat seat : seats) {
            if (validateLock(show, seat, userId)) {
                unlockSeat(show, seat);
            }
        }
    }

    public void confirmLock(Show show, Seat seat, String userId) {
        synchronized (seat) {
            Map<String, SeatLock> showLocks = locks.get(show.getId());
            if (showLocks != null) {
                showLocks.remove(seat.getId());
            }
        }
    }

    private void unlockSeat(Show show, Seat seat) {
        synchronized (seat) {
            if (seat.getStatus() == SeatStatus.LOCKED) {
                seat.setStatus(SeatStatus.AVAILABLE);
            }
            Map<String, SeatLock> showLocks = locks.get(show.getId());
            if (showLocks != null) {
                showLocks.remove(seat.getId());
            }
        }
    }

    public boolean validateLock(Show show, Seat seat, String userId) {
        Map<String, SeatLock> showLocks = locks.get(show.getId());
        if (showLocks != null && showLocks.containsKey(seat.getId())) {
            SeatLock lock = showLocks.get(seat.getId());
            return lock.getUserId().equals(userId) && !lock.isExpired();
        }
        return false;
    }

    private void cleanExpiredLocks() {
        try {
            for (Map.Entry<String, Map<String, SeatLock>> showEntry : locks.entrySet()) {
                Map<String, SeatLock> showLocks = showEntry.getValue();
                for (Map.Entry<String, SeatLock> lockEntry : showLocks.entrySet()) {
                    SeatLock lock = lockEntry.getValue();
                    if (lock.isExpired()) {
                        unlockSeat(lock.getShow(), lock.getSeat());
                        System.out.println("[System]: Lock expired and released for seat: " + lock.getSeat().getId()
                                + " at Show " + lock.getShow().getId());
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("Scheduler Error: " + t.getMessage());
            t.printStackTrace();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}

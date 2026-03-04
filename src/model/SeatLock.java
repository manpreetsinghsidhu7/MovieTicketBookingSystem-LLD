package model;

import java.time.Instant;

public class SeatLock {
    private final Seat seat;
    private final Show show;
    private final String userId;
    private final Instant lockTime;
    private final Instant expirationTime;

    public SeatLock(Seat seat, Show show, String userId, int timeoutInSeconds) {
        this.seat = seat;
        this.show = show;
        this.userId = userId;
        this.lockTime = Instant.now();
        this.expirationTime = this.lockTime.plusSeconds(timeoutInSeconds);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expirationTime);
    }

    public Seat getSeat() {
        return seat;
    }

    public Show getShow() {
        return show;
    }

    public String getUserId() {
        return userId;
    }
}

package model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Show {
    private final String id;
    private final String movieId;
    private final Screen screen;
    private final LocalDateTime startTime;
    private final Map<String, Seat> seats = new ConcurrentHashMap<>();

    public Show(String id, String movieId, Screen screen, LocalDateTime startTime) {
        this.id = id;
        this.movieId = movieId;
        this.screen = screen;
        this.startTime = startTime;
    }

    public void addSeat(Seat seat) {
        seats.put(seat.getId(), seat);
    }

    public Seat getSeat(String seatId) {
        return seats.get(seatId);
    }

    public Map<String, Seat> getSeats() {
        return seats;
    }

    public String getId() {
        return id;
    }

    public Screen getScreen() {
        return screen;
    }

    public String getMovieId() {
        return movieId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
}

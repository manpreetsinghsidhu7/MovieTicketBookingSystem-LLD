package model;

public class Seat {
    private final String id;
    private final SeatType type;
    private SeatStatus status;

    public Seat(String id, SeatType type) {
        this.id = id;
        this.type = type;
        this.status = SeatStatus.AVAILABLE;
    }

    public String getId() {
        return id;
    }

    public SeatType getType() {
        return type;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }
}

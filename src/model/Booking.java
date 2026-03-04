package model;

import java.util.List;

public class Booking {
    private final String id;
    private final String userId;
    private final Show show;
    private final List<Seat> seats;
    private final double amountPaid;

    public Booking(String id, String userId, Show show, List<Seat> seats, double amountPaid) {
        this.id = id;
        this.userId = userId;
        this.show = show;
        this.seats = seats;
        this.amountPaid = amountPaid;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public Show getShow() {
        return show;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public double getAmountPaid() {
        return amountPaid;
    }
}

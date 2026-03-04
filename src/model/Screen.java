package model;

public class Screen {
    private final String id;
    private final Theatre theatre;

    public Screen(String id, Theatre theatre) {
        this.id = id;
        this.theatre = theatre;
    }

    public String getId() {
        return id;
    }

    public Theatre getTheatre() {
        return theatre;
    }
}

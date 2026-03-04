package model;

public class Theatre {
    private final String id;
    private final String name;

    public Theatre(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

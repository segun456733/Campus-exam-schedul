package com.campus.examscheduler.model;

/** An examination venue (hall, theatre, classroom). */
public class Venue {
    private long id;
    private String name;
    private int capacity;
    private String type = "";
    private boolean available = true;
    private String notes = "";

    public Venue() { }

    public Venue(long id, String name, int capacity, String type, boolean available) {
        this.id = id; this.name = name; this.capacity = capacity; this.type = type; this.available = available;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type == null ? "" : type; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes == null ? "" : notes; }

    @Override public String toString() { return name; }
}

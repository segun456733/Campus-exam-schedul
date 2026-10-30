package com.campus.examscheduler.model;

/** Administrator account (password is stored only as a salted hash). */
public class Admin {
    private final long id;
    private final String username;
    private final String passwordHash;
    private final String salt;
    private final int iterations;

    public Admin(long id, String username, String passwordHash, String salt, int iterations) {
        this.id = id; this.username = username; this.passwordHash = passwordHash;
        this.salt = salt; this.iterations = iterations;
    }
    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
    public int getIterations() { return iterations; }
}

package com.campus.examscheduler.model;

/** A department inside the School/Faculty of Science. */
public class Department {
    private long id;
    private String code;
    private String name;
    private String description = "";
    private int courseCount; // filled by queries, not stored

    public Department() { }

    public Department(long id, String code, String name, String description) {
        this.id = id; this.code = code; this.name = name; this.description = description;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public int getCourseCount() { return courseCount; }
    public void setCourseCount(int courseCount) { this.courseCount = courseCount; }

    @Override public String toString() { return name; }
}

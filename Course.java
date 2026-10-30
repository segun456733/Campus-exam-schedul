package com.campus.examscheduler.model;

import java.util.ArrayList;
import java.util.List;

/** A course with an examination. */
public class Course {
    private long id;
    private String code;
    private String title;
    private long departmentId;
    private String departmentName = "";
    private String level = "ND1";
    private int studentCount;
    private int durationMinutes = 120;
    private long groupId;
    private String groupName = "";
    private List<Long> extraGroupIds = new ArrayList<>();
    private List<String> extraGroupNames = new ArrayList<>();
    private Long jointExamId;
    private String jointName = "";

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getDepartmentId() { return departmentId; }
    public void setDepartmentId(long departmentId) { this.departmentId = departmentId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public long getGroupId() { return groupId; }
    public void setGroupId(long groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public List<Long> getExtraGroupIds() { return extraGroupIds; }
    public void setExtraGroupIds(List<Long> ids) { this.extraGroupIds = ids; }
    public List<String> getExtraGroupNames() { return extraGroupNames; }
    public void setExtraGroupNames(List<String> names) { this.extraGroupNames = names; }
    public Long getJointExamId() { return jointExamId; }
    public void setJointExamId(Long jointExamId) { this.jointExamId = jointExamId; }
    public String getJointName() { return jointName; }
    public void setJointName(String jointName) { this.jointName = jointName == null ? "" : jointName; }

    @Override public String toString() { return code + " - " + title; }
}

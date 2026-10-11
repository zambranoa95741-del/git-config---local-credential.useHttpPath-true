package com.classroomplus.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class ScheduleSlot {

    private final int id;
    private final int classroomId;
    private DayOfWeek day;
    private LocalTime startTime;
    private LocalTime endTime;
    private String subject;
    private String instructor;

    public ScheduleSlot(int id, int classroomId, DayOfWeek day, LocalTime startTime,
                        LocalTime endTime, String subject, String instructor) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        this.id = id;
        this.classroomId = classroomId;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.subject = subject;
        this.instructor = instructor;
    }

    public int getId() { return id; }
    public int getClassroomId() { return classroomId; }
    public DayOfWeek getDay() { return day; }
    public void setDay(DayOfWeek day) { this.day = day; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }

    public void setTimes(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getTimeLabel() {
        return com.classroomplus.util.TimeUtil.range(startTime, endTime);
    }
}
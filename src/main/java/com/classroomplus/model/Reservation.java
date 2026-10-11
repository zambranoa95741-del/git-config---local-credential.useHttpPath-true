package com.classroomplus.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Reservation {

    private final int id;
    private final Classroom classroom;
    private final User reservedBy;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purpose;
    private boolean cancelled;

    public Reservation(int id, Classroom classroom, User reservedBy, LocalDate date,
                       LocalTime startTime, LocalTime endTime, String purpose) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        this.id = id;
        this.classroom = classroom;
        this.reservedBy = reservedBy;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
    }

    public int getId() { return id; }
    public Classroom getClassroom() { return classroom; }
    public User getReservedBy() { return reservedBy; }
    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public boolean isCancelled() { return cancelled; }
    public void cancel() { this.cancelled = true; }

    public void reschedule(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** Status is computed from the clock, so reservations complete automatically. */
    public ReservationStatus getStatus() {
        return getStatus(LocalDateTime.now());
    }

    public ReservationStatus getStatus(LocalDateTime now) {
        if (cancelled) {
            return ReservationStatus.CANCELLED;
        }
        if (now.isBefore(LocalDateTime.of(date, startTime))) {
            return ReservationStatus.UPCOMING;
        }
        if (now.isBefore(LocalDateTime.of(date, endTime))) {
            return ReservationStatus.ONGOING;
        }
        return ReservationStatus.COMPLETED;
    }

    // Convenience getters for table columns
    public String getRoomLabel() { return classroom.toString(); }
    public String getTimeLabel() { return com.classroomplus.util.TimeUtil.range(startTime, endTime); }
}
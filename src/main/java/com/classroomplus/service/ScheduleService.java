package com.classroomplus.service;

import com.classroomplus.model.ScheduleSlot;
import com.classroomplus.util.TimeUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

public class ScheduleService {

    private static final LocalTime OPENING_TIME = LocalTime.of(7, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(21, 0);

    private static final ScheduleService INSTANCE = new ScheduleService();

    public static ScheduleService getInstance() {
        return INSTANCE;
    }

    private final ObservableList<ScheduleSlot> slots = FXCollections.observableArrayList();
    private int nextId = 1;

    private ScheduleService() {
    }

    /** Read: all slots, for tables that bind to the list. */
    public ObservableList<ScheduleSlot> getSlots() {
        return FXCollections.unmodifiableObservableList(slots);
    }

    /** Read: the weekly schedule of one room, ordered by day and start time. */
    public List<ScheduleSlot> getSlotsFor(int classroomId) {
        return slots.stream()
                .filter(slot -> slot.getClassroomId() == classroomId)
                .sorted(Comparator.comparing(ScheduleSlot::getDay)
                        .thenComparing(ScheduleSlot::getStartTime))
                .toList();
    }

    /** True if the room already has a regular class overlapping this day and time. */
    public boolean isOccupied(int classroomId, DayOfWeek day, LocalTime start, LocalTime end) {
        return findConflict(classroomId, day, start, end, null) != null;
    }

    /** Create: throws IllegalArgumentException with a readable message if the input is invalid. */
    public ScheduleSlot add(int classroomId, DayOfWeek day, LocalTime start, LocalTime end,
                            String subject, String instructor) {
        validate(classroomId, day, start, end, subject, null);
        ScheduleSlot slot = new ScheduleSlot(nextId++, classroomId, day, start, end,
                subject.trim(), clean(instructor));
        slots.add(slot);
        return slot;
    }

    /** Update: edits an existing slot. */
    public void update(ScheduleSlot slot, DayOfWeek day, LocalTime start, LocalTime end,
                       String subject, String instructor) {
        validate(slot.getClassroomId(), day, start, end, subject, slot);
        slot.setDay(day);
        slot.setTimes(start, end);
        slot.setSubject(subject.trim());
        slot.setInstructor(clean(instructor));
    }

    /** Delete: removes one slot. */
    public void delete(ScheduleSlot slot) {
        slots.remove(slot);
    }

    /** Delete: removes every slot of a room (used when the room itself is deleted). */
    public void deleteForClassroom(int classroomId) {
        slots.removeIf(slot -> slot.getClassroomId() == classroomId);
    }

    private void validate(int classroomId, DayOfWeek day, LocalTime start, LocalTime end,
                          String subject, ScheduleSlot ignore) {
        if (day == null) {
            throw new IllegalArgumentException("Day is required.");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end time are required.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        if (start.isBefore(OPENING_TIME) || end.isAfter(CLOSING_TIME)) {
            throw new IllegalArgumentException("Schedules must be between 7:00 AM and 9:00 PM.");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject is required.");
        }
        ScheduleSlot conflict = findConflict(classroomId, day, start, end, ignore);
        if (conflict != null) {
            throw new IllegalArgumentException("This room already has " + conflict.getSubject()
                    + " at " + conflict.getTimeLabel() + " on that day.");
        }
    }

    private ScheduleSlot findConflict(int classroomId, DayOfWeek day, LocalTime start,
                                      LocalTime end, ScheduleSlot ignore) {
        return slots.stream()
                .filter(slot -> slot != ignore)
                .filter(slot -> slot.getClassroomId() == classroomId)
                .filter(slot -> slot.getDay() == day)
                .filter(slot -> TimeUtil.overlaps(start, end, slot.getStartTime(), slot.getEndTime()))
                .findFirst()
                .orElse(null);
    }

    private String clean(String text) {
        return text == null ? "" : text.trim();
    }
}
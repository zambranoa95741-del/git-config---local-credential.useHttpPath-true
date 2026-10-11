package com.classroomplus.service;

import com.classroomplus.model.Classroom;
import com.classroomplus.model.Reservation;
import com.classroomplus.model.ReservationStatus;
import com.classroomplus.model.ScheduleSlot;
import com.classroomplus.model.User;
import com.classroomplus.util.TimeUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ReservationService {

    private static final LocalTime OPENING_TIME = LocalTime.of(7, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(21, 0);
    private static final int IMMEDIATE_CHECK_MINUTES = 30;

    private static final ReservationService INSTANCE = new ReservationService();

    public static ReservationService getInstance() {
        return INSTANCE;
    }

    private final ObservableList<Reservation> reservations = FXCollections.observableArrayList();
    private final ScheduleService scheduleService = ScheduleService.getInstance();
    private final ClassroomService classroomService = ClassroomService.getInstance();
    private int nextId = 1;

    private ReservationService() {
    }

    // ------------------------------------------------------------------ Read

    public ObservableList<Reservation> getReservations() {
        return FXCollections.unmodifiableObservableList(reservations);
    }

    /** A user's reservations, newest first (upcoming and previous, F-4.8). */
    public List<Reservation> getReservationsFor(User user) {
        return reservations.stream()
                .filter(r -> r.getReservedBy().getId() == user.getId())
                .sorted(Comparator.comparing(Reservation::getDate)
                        .thenComparing(Reservation::getStartTime)
                        .reversed())
                .toList();
    }

    public long countFor(User user, ReservationStatus status) {
        return reservations.stream()
                .filter(r -> r.getReservedBy().getId() == user.getId())
                .filter(r -> r.getStatus() == status)
                .count();
    }

    public boolean hasActiveReservations(Classroom room) {
        return reservations.stream()
                .filter(r -> r.getClassroom().getId() == room.getId())
                .anyMatch(r -> r.getStatus() == ReservationStatus.UPCOMING
                        || r.getStatus() == ReservationStatus.ONGOING);
    }

    // ---------------------------------------------------------- Availability

    /**
     * Compares the requested time against the semester schedule and existing
     * reservations. Returns the reason if the room is taken, or empty if it is free.
     */
    public Optional<String> findConflict(Classroom room, LocalDate date, LocalTime start,
                                         LocalTime end, Reservation ignore) {
        for (ScheduleSlot slot : scheduleService.getSlotsFor(room.getId())) {
            if (slot.getDay() == date.getDayOfWeek()
                    && TimeUtil.overlaps(start, end, slot.getStartTime(), slot.getEndTime())) {
                return Optional.of("Regular class: " + slot.getSubject()
                        + " (" + slot.getTimeLabel() + ")");
            }
        }
        for (Reservation r : reservations) {
            if (r == ignore || r.isCancelled()) {
                continue;
            }
            if (r.getClassroom().getId() == room.getId()
                    && r.getDate().equals(date)
                    && TimeUtil.overlaps(start, end, r.getStartTime(), r.getEndTime())) {
                return Optional.of("Already reserved " + r.getTimeLabel()
                        + " for " + r.getPurpose());
            }
        }
        return Optional.empty();
    }

    public boolean isAvailable(Classroom room, LocalDate date, LocalTime start, LocalTime end) {
        return findConflict(room, date, start, end, null).isEmpty();
    }

    /** Free right now for at least the next 30 minutes (or until closing time). */
    public boolean isAvailableNow(Classroom room) {
        LocalTime start = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        if (start.isBefore(OPENING_TIME) || !start.isBefore(CLOSING_TIME)) {
            return false;
        }
        LocalTime end = start.plusMinutes(IMMEDIATE_CHECK_MINUTES);
        if (end.isBefore(start) || end.isAfter(CLOSING_TIME)) {
            end = CLOSING_TIME;
        }
        return isAvailable(room, LocalDate.now(), start, end);
    }

    /** Other rooms that are free at the requested time (F-3.6). */
    public List<Classroom> findAlternatives(Classroom excluded, LocalDate date, LocalTime start,
                                            LocalTime end, int minCapacity) {
        return classroomService.getClassrooms().stream()
                .filter(room -> excluded == null || room.getId() != excluded.getId())
                .filter(room -> room.getCapacity() >= minCapacity)
                .filter(room -> isAvailable(room, date, start, end))
                .toList();
    }

    // ---------------------------------------------------------------- Create

    /** Advance reservation. Throws IllegalArgumentException with a readable message. */
    public Reservation reserve(Classroom room, User user, LocalDate date, LocalTime start,
                               LocalTime end, String purpose) {
        if (user.isStudent()) {
            throw new IllegalArgumentException("Students can view classrooms but cannot make reservations.");
        }
        validateRequest(room, date, start, end, purpose);
        Optional<String> conflict = findConflict(room, date, start, end, null);
        if (conflict.isPresent()) {
            throw new IllegalArgumentException("This room is not available. " + conflict.get());
        }
        Reservation reservation = new Reservation(nextId++, room, user, date, start, end,
                purpose.trim());
        reservations.add(reservation);
        return reservation;
    }

    /** Immediate reservation: starts now and lasts the given number of minutes. */
    public Reservation reserveNow(Classroom room, User user, int minutes, String purpose) {
        if (minutes < 15) {
            throw new IllegalArgumentException("Choose at least 15 minutes.");
        }
        LocalTime start = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalTime end = start.plusMinutes(minutes);
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("It is too late to reserve for today.");
        }
        return reserve(room, user, LocalDate.now(), start, end, purpose);
    }

    // ---------------------------------------------------------------- Update

    public void update(Reservation reservation, User actor, LocalDate date, LocalTime start,
                       LocalTime end, String purpose) {
        requireOwner(reservation, actor);
        if (reservation.getStatus() != ReservationStatus.UPCOMING) {
            throw new IllegalArgumentException("Only upcoming reservations can be edited.");
        }
        validateRequest(reservation.getClassroom(), date, start, end, purpose);
        Optional<String> conflict = findConflict(reservation.getClassroom(), date, start, end,
                reservation);
        if (conflict.isPresent()) {
            throw new IllegalArgumentException("This room is not available. " + conflict.get());
        }
        reservation.reschedule(date, start, end);
        reservation.setPurpose(purpose.trim());
    }

    // ---------------------------------------------------------------- Delete

    /** Cancels (deletes) an upcoming reservation (F-4.7). */
    public void cancel(Reservation reservation, User actor) {
        requireOwner(reservation, actor);
        ReservationStatus status = reservation.getStatus();
        boolean allowed = status == ReservationStatus.UPCOMING
                || (actor.isAdmin() && status == ReservationStatus.ONGOING);
        if (!allowed) {
            throw new IllegalArgumentException(actor.isAdmin()
                    ? "Only upcoming or ongoing reservations can be cancelled."
                    : "Only upcoming reservations can be cancelled.");
        }
        reservation.cancel();
    }

    // --------------------------------------------------------------- Helpers

    private void validateRequest(Classroom room, LocalDate date, LocalTime start,
                                 LocalTime end, String purpose) {
        if (room == null) {
            throw new IllegalArgumentException("Select a classroom.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Select a date.");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Select a start and end time.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        if (start.isBefore(OPENING_TIME) || end.isAfter(CLOSING_TIME)) {
            throw new IllegalArgumentException("Reservations must be between 7:00 AM and 9:00 PM.");
        }
        if (!LocalDateTime.of(date, end).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("That time has already passed.");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Purpose is required.");
        }
    }

    private void requireOwner(Reservation reservation, User actor) {
        if (!actor.isAdmin() && reservation.getReservedBy().getId() != actor.getId()) {
            throw new IllegalArgumentException("You can only change your own reservations.");
        }
    }
}
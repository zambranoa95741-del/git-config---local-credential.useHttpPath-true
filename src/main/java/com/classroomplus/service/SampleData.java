package com.classroomplus.service;

import com.classroomplus.model.Classroom;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Pre-loads classrooms and semester schedules when the application starts. */
public final class SampleData {

    private static final String BUILDING = "WAC Building";

    private static final DayOfWeek[] MON_THU = {DayOfWeek.MONDAY, DayOfWeek.THURSDAY};
    private static final DayOfWeek[] TUE_FRI = {DayOfWeek.TUESDAY, DayOfWeek.FRIDAY};

    private static boolean loaded;

    private SampleData() {
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;

        ClassroomService rooms = ClassroomService.getInstance();
        ScheduleService schedules = ScheduleService.getInstance();

        // Rooms from the enrollment schedule (capacity and facilities are placeholders)
        Classroom wac203 = rooms.add("203", BUILDING, 40, "Whiteboard");
        Classroom wac301 = rooms.add("301", BUILDING, 40, "Whiteboard");
        Classroom wac302 = rooms.add("302", BUILDING, 40, "Whiteboard");

        // Regular classes from the enrollment schedule (BSIT2-1)
        addClass(schedules, wac301, MON_THU, "08:00", "09:30",
                "BSIT-2101 Object Oriented Programming");
        addClass(schedules, wac302, MON_THU, "14:30", "16:00",
                "BSIT-2103 Data Structure and Algorithms");
        addClass(schedules, wac302, TUE_FRI, "17:30", "19:00",
                "BSIT-2102 Platform Technologies");
        addClass(schedules, wac203, TUE_FRI, "13:00", "14:30",
                "BSIT-2104 Networking 1");
    }

    private static void addClass(ScheduleService service, Classroom room, DayOfWeek[] days,
                                 String start, String end, String subject) {
        for (DayOfWeek day : days) {
            service.add(room.getId(), day, LocalTime.parse(start), LocalTime.parse(end),
                    subject, "");
        }
    }
}
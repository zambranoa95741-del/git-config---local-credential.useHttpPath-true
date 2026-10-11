package com.classroomplus.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TimeUtil {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private TimeUtil() {
    }

    /** Two time ranges overlap if each one starts before the other ends. */
    public static boolean overlaps(LocalTime startA, LocalTime endA,
                                   LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    public static String format(LocalTime time) {
        return time.format(TIME_FORMAT);
    }

    public static String range(LocalTime start, LocalTime end) {
        return format(start) + " - " + format(end);
    }
}
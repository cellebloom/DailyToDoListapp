package com.example.dailytodolist.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * One day cell in the horizontal day selector above the timeline.
 *
 * <p>Kept as pure Java (no Android imports) so the week maths can be unit
 * tested without an emulator. Date formatting is done with
 * {@link SimpleDateFormat} rather than by hand, so leap years and month
 * boundaries are correct automatically.</p>
 */
public final class WeekDay {

    /** Storage/format pattern used everywhere for a calendar day. */
    public static final String ISO_PATTERN = "yyyy-MM-dd";

    private final String dateIso;
    /** 0 = Monday … 6 = Sunday, i.e. {@link Calendar#MONDAY} based. */
    private final int dayOfWeekIndex;
    private final int dayOfMonth;
    private final boolean today;

    public WeekDay(String dateIso, int dayOfWeekIndex, int dayOfMonth, boolean today) {
        this.dateIso = dateIso;
        this.dayOfWeekIndex = dayOfWeekIndex;
        this.dayOfMonth = dayOfMonth;
        this.today = today;
    }

    public String getDateIso() {
        return dateIso;
    }

    public int getDayOfWeekIndex() {
        return dayOfWeekIndex;
    }

    /** Day of month as a zero-padded two-character string, e.g. {@code "04"}. */
    public String getDayOfMonthLabel() {
        return String.format(Locale.US, "%02d", dayOfMonth);
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public boolean isToday() {
        return today;
    }

    /**
     * Builds the Monday-to-Sunday week that contains {@code anchorIso}.
     *
     * <p>Weeks are Monday-first to match the physical planners the layout is
     * modelled on; {@link Calendar} defaults to Sunday-first in some locales,
     * so the first day of week is pinned rather than read from the device.</p>
     *
     * @param anchorIso a date in {@code yyyy-MM-dd}; the week containing it
     * @param todayIso  the date considered "today", flagged for highlighting
     * @return seven days, Monday first; never null
     * @throws IllegalArgumentException if either argument cannot be parsed
     */
    public static List<WeekDay> weekOf(String anchorIso, String todayIso) {
        SimpleDateFormat format = new SimpleDateFormat(ISO_PATTERN, Locale.US);
        format.setLenient(false);

        Date anchor;
        try {
            anchor = format.parse(anchorIso);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unparseable anchor date: " + anchorIso, e);
        }
        if (anchor == null) {
            throw new IllegalArgumentException("Unparseable anchor date: " + anchorIso);
        }

        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.setTime(anchor);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        // Monday-first index, 0..6
        int mondayBased = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        calendar.add(Calendar.DAY_OF_MONTH, -mondayBased);

        List<WeekDay> days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            String iso = format.format(calendar.getTime());
            days.add(new WeekDay(
                    iso,
                    i,
                    calendar.get(Calendar.DAY_OF_MONTH),
                    iso.equals(todayIso)));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }
}

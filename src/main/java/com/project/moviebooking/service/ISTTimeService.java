package com.project.moviebooking.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ISTTimeService — Centralised Indian Standard Time (IST) utility.
 *
 * WHY IST IS NEEDED:
 * Servers typically run on UTC (Coordinated Universal Time), which is
 * 5 hours 30 minutes BEHIND Indian Standard Time. Without explicit IST
 * handling, a show at 10:00 AM IST would appear "past" on a UTC server
 * at 04:30 UTC — causing valid morning shows to be incorrectly hidden.
 *
 * OOAD: GRASP Information Expert — this class owns all time-related
 * knowledge. No other class should calculate IST independently (Low Coupling).
 *
 * SOLID: SRP — this class has exactly ONE responsibility: IST time logic.
 */
@Component
public class ISTTimeService {

    /** IST Zone — Asia/Kolkata = UTC+5:30 */
    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    /** Returns the current ZonedDateTime anchored to IST */
    public ZonedDateTime nowIST() {
        return ZonedDateTime.now(IST);
    }

    /** Returns today's date in IST — different from LocalDate.now() (UTC) */
    public String todayIST() {
        return nowIST().toLocalDate().toString();
    }

    /** Returns current IST time */
    public LocalTime currentTimeIST() {
        return nowIST().toLocalTime();
    }

    /** Full readable IST string e.g. "19 Apr 2026, 5:06 PM IST" */
    public String currentISTString() {
        return nowIST().format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a 'IST'"));
    }

    /**
     * Core bookability check using ZonedDateTime — fully IST-aware.
     *
     * ALGORITHM (exact BookMyShow logic):
     *   1. Parse showDate + showTime into a LocalDateTime
     *   2. Attach IST zone → ZonedDateTime showIST
     *   3. Add 15-minute grace → showIST.plusMinutes(15)
     *   4. Return whether nowIST is BEFORE that cutoff
     *
     * This means:
     *   Past date                    → false  (nowIST is after showIST + 15m)
     *   Future date                  → true   (nowIST is before showIST + 15m)
     *   Today, show time passed 15m  → false
     *   Today, show time upcoming    → true
     *   Today, show time ongoing     → true (within 15-min grace)
     *
     * @param showDate "2026-04-19"  (ISO date string)
     * @param showTime "14:00"       (HH:mm or HH:mm:ss — first 5 chars used)
     */
    public boolean isShowBookable(String showDate, String showTime) {
        try {
            LocalDate  showLocalDate = LocalDate.parse(showDate);
            LocalTime  showLocalTime = LocalTime.parse(showTime.substring(0, 5));

            // Build show as a ZonedDateTime in IST
            ZonedDateTime showIST = LocalDateTime
                    .of(showLocalDate, showLocalTime)
                    .atZone(IST);

            ZonedDateTime nowIST = ZonedDateTime.now(IST);

            // Bookable until 15 minutes AFTER show starts (BookMyShow grace period)
            return nowIST.isBefore(showIST.plusMinutes(15));

        } catch (Exception e) {
            return false; // Unparseable input → treat as expired
        }
    }

    /**
     * Returns minutes from nowIST until show starts.
     * Negative value = show already started.
     */
    public long minutesUntilShow(String showDate, String showTime) {
        try {
            LocalDate showLocalDate = LocalDate.parse(showDate);
            LocalTime showLocalTime = LocalTime.parse(showTime.substring(0, 5));
            ZonedDateTime showIST   = LocalDateTime.of(showLocalDate, showLocalTime).atZone(IST);
            return java.time.Duration.between(ZonedDateTime.now(IST), showIST).toMinutes();
        } catch (Exception e) {
            return Long.MIN_VALUE;
        }
    }

    /**
     * Returns full hours from nowIST until show.
     * Negative = show already started. Used by RefundService.
     */
    public long hoursUntilShow(String showDate, String showTime) {
        return minutesUntilShow(showDate, showTime) / 60;
    }

    /**
     * Human-readable show status label for the frontend slot buttons.
     *
     * Returns:
     *   "EXPIRED"        past show (> 15 min after start)
     *   "STARTED"        within 15 min grace
     *   "Starting soon"  < 15 min until start
     *   "In 45 min"      < 2h
     *   "In 3h 20m"      > 2h, same day
     *   "Tomorrow"       next day
     *   "In 2 days"      day after
     */
    public String getShowStatus(String showDate, String showTime) {
        try {
            LocalDate date     = LocalDate.parse(showDate);
            LocalDate todayIST = ZonedDateTime.now(IST).toLocalDate();
            long minutes       = minutesUntilShow(showDate, showTime);

            if (minutes < -15) return "EXPIRED";
            if (minutes < 0)   return "STARTED";
            if (minutes < 15)  return "Starting soon";
            if (minutes < 60)  return "In " + minutes + " min";

            if (date.equals(todayIST.plusDays(1))) return "Tomorrow";
            if (date.equals(todayIST.plusDays(2))) return "In 2 days";

            long hours = minutes / 60;
            long mins  = minutes % 60;
            return "In " + hours + "h" + (mins > 0 ? " " + mins + "m" : "");
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Converts 24-hour time to 12-hour AM/PM.
     * "14:00" → "2:00 PM" | "10:00" → "10:00 AM"
     */
    public String formatTo12Hour(String time24) {
        try {
            LocalTime t = LocalTime.parse(time24.substring(0, 5));
            return t.format(DateTimeFormatter.ofPattern("h:mm a"));
        } catch (Exception e) {
            return time24;
        }
    }
}

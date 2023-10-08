package com.gunnarro.android.terex.utility;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class Utility {

    public static final Integer DEFAULT_DAILY_BREAK_IN_MINUTES = 30;
    public static final Long DEFAULT_DAILY_WORKING_HOURS_IN_MINUTES = 8 * 60L - DEFAULT_DAILY_BREAK_IN_MINUTES;
    public static final Integer DEFAULT_HOURLY_RATE = 1075;
    private static final SimpleDateFormat dateFormatter;
    private static final String DATE_TIME_PATTERN = "dd-MM-yyyy HH:mm";
    public static final String WORKDAY_DATE_PATTERN = "EEE d MMM";
    private static final String DATE_PATTERN = "dd-MM-yyyy";
    private static final String TIME_PATTERN = "HH:mm";
    private static String currentUUID;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
            .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
            .registerTypeAdapter(LocalTime.class, new LocalTimeTypeAdapter())
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .setPrettyPrinting()
            .create();

    public static Gson gsonMapper() {
        return gson;
    }

    public static DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    private Utility() {
        genNewUUID();
    }

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN, Locale.getDefault());

    static {
        dateFormatter = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault());
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static void genNewUUID() {
        currentUUID = java.util.UUID.randomUUID().toString();
    }

    public static String buildTag(Class<?> clazz, String tagName) {
        return new StringBuilder(clazz.getSimpleName())
                .append(".").append(tagName)
                .append(", UUID=").append(currentUUID)
                .append(", thread=").append(Thread.currentThread().getName())
                .toString();
    }

    public static Date toDate(String dateStr) {
        try {
            return dateFormatter.parse(dateStr);
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }

    public static String formatTime(long timeMs) {
        if (timeMs >= 0) {
            return dateFormatter.format(new Date(timeMs));
        }
        return "";
    }

    public static String formatDateTime(LocalDateTime localDateTime) {
        if (localDateTime != null) {
            return localDateTime.format(dateTimeFormatter);
        }
        return null;
    }

    public static DateTimeFormatter getDateFormatter() {
        return DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.getDefault());
    }

    public static DateTimeFormatter getTimeFormatter() {
        return DateTimeFormatter.ofPattern(TIME_PATTERN, Locale.getDefault());
    }

    public static LocalDate toLocalDate(String dateStr) {
        Log.d("Utility", "toLocalDate: " + dateStr);
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.getDefault()));
    }

    public static LocalTime toLocalTime(String timeStr) {
        return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern(TIME_PATTERN, Locale.getDefault()));
    }

    public static String formatTime(LocalTime localTime) {
        return localTime != null ? localTime.format(DateTimeFormatter.ofPattern(TIME_PATTERN, Locale.getDefault())) : null;
    }

    public static String formatDate(LocalDate localDate) {
        if (localDate != null) {
            return localDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN));
        }
        return null;
    }

    public static String getDateDiffInHours(LocalTime d1, LocalTime d2) {
        if (d1 != null && d2 != null) {
            int diffHour = d2.getHour() - d1.getHour();
            int diffMinute = d2.getMinute() - d1.getMinute();
            return String.format("%s.%s", diffHour, new DecimalFormat("#").format((double) diffMinute / 60 * 10));
        }
        return null;
    }

    public static String formatToHHMM(int hour, int minute) {
        String hh = String.format("%s", hour);
        String mm = String.format("%s", minute);
        if (hour >= 0 && hour < 10) {
            hh = String.format("0%s", hour);
        }
        if (minute >= 0 && minute < 10) {
            mm = String.format("0%s", minute);
        }
        return String.format("%s:%s", hh, mm);
    }

    ;

    public static String formatToDDMMYYYY(int year, int month, int day) {
        String dd = String.format("%s", day);
        String mm = String.format("%s", month);
        if (day >= 0 && day < 10) {
            dd = String.format("0%s", day);
        }
        if (month >= 0 && month < 10) {
            mm = String.format("0%s", month);
        }
        return String.format("%s-%s-%s", dd, mm, year);
    }


    private static final Pattern POSITIVE_INTEGER_PATTERN = Pattern.compile("\\d+");

    public static boolean isInteger(String value) {
        if (value == null) {
            return false;
        }
        return POSITIVE_INTEGER_PATTERN.matcher(value).matches();
    }

    public static LocalDateTime toLocalDateTime(String dateTimeStr) {
        Log.d("Utility", "toLocalDateTime: " + dateTimeStr);
        if (dateTimeStr == null || dateTimeStr.length() != DATE_TIME_PATTERN.length()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, dateTimeFormatter);
    }

    public static void getFirstDayOfWeek() {
        LocalDate today = LocalDate.now();

        // Go backward to get Monday
        LocalDate monday = today;
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.minusDays(1);
        }

        // Go forward to get Sunday
        LocalDate sunday = today;
        while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
            sunday = sunday.plusDays(1);
        }

        System.out.println("Today: " + today);
        System.out.println("Monday of the Week: " + monday);
        System.out.println("Sunday of the Week: " + sunday);
    }

    public static LocalDate getFirstDayOfWeek(LocalDate date, Integer weekOfYear) {
        return LocalDate.of(date.getYear(), date.getMonthValue(), 1)
                .with(DayOfWeek.MONDAY)
                .with(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear(), weekOfYear);
    }

    /**
     * Use Monday as start of week
     */
    public static LocalDate getFirstDayOfWeek(Integer year, Integer month, Integer weekOfYear) {
        return LocalDate.of(year, month, 1)
                .with(DayOfWeek.MONDAY)
                .with(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear(), weekOfYear);
    }

    /**
     * use sunday as last day of week
     */
    public static LocalDate getLastDayOfWeek(LocalDate date, Integer weekOfYear) {
        LocalDate firstDayOfWeek = getFirstDayOfWeek(date.getYear(), date.getMonthValue(), weekOfYear);
        while (firstDayOfWeek.getDayOfWeek() != DayOfWeek.SUNDAY) {
            firstDayOfWeek = firstDayOfWeek.plusDays(1);
        }
        return firstDayOfWeek;
    }

    public static Integer getWeek(LocalDate date) {
        return date.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
    }

    public static LocalDate getFirstDayOfMonth(LocalDate date) {
        return LocalDate.of(date.getYear(), date.getMonth(), 1);
    }

    public static LocalDate getLastDayOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    public static String[] getYears() {
        return new String[]{"2023", "2024"};
    }

    public static String[] getMonths() {
        return new DateFormatSymbols().getMonths();
    }

    /**
     * go from 0 - 11
     */
    public static String mapMonthNumberToName(Integer monthNumber) {
        return getMonths()[monthNumber];
    }

    public static Integer mapMonthNameToNumber(String monthName) {
        String[] months = getMonths();
        for (int i = 0; i < months.length; i++) {
            if (months[i].equalsIgnoreCase(monthName)) {
                return i;
            }
        }
        return null;
    }
}
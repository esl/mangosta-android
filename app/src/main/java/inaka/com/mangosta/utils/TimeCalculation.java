package inaka.com.mangosta.utils;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.Date;
import java.util.Locale;

import inaka.com.mangosta.R;

public class TimeCalculation {

    public static String getTimeStringAgoSinceDate(Context context, Date date) {
        DateTime postDate = new DateTime(date, DateTimeZone.getDefault());
        return getTimeStringAgoSinceDateTime(context, postDate);
    }

    private static String getTimeStringAgoSinceDateTime(Context context, DateTime dateTime) {
        DateTime now = new DateTime(DateTimeZone.getDefault());
        Period period = new Period(dateTime, now);
        String date;
        int count;

        if (period.getYears() >= 1) {
            date = context.getResources().getQuantityString(R.plurals.date_years_ago, period.getYears());
            count = period.getYears();
        } else if (period.getMonths() >= 1) {
            date = context.getResources().getQuantityString(R.plurals.date_months_ago, period.getMonths());
            count = period.getMonths();
        } else if (period.getWeeks() >= 1) {
            date = context.getResources().getQuantityString(R.plurals.date_weeks_ago, period.getWeeks());
            count = period.getWeeks();
        } else if (period.getDays() >= 1) {
            date = context.getResources().getQuantityString(R.plurals.date_days_ago, period.getDays());
            count = period.getDays();
        } else if (period.getHours() >= 1) {
            date = context.getResources().getQuantityString(R.plurals.date_hours_ago, period.getHours());
            count = period.getHours();
        } else if (period.getMinutes() >= 1) {
            date = context.getResources().getQuantityString(R.plurals.date_minutes_ago, period.getMinutes());
            count = period.getMinutes();
        } else if (period.getSeconds() >= 3) {
            date = String.format(Locale.getDefault(), context.getString(R.string.date_seconds_ago), period.getSeconds());
            count = period.getSeconds();
        } else {
            return " " + context.getString(R.string.date_seconds_now);
        }

        return String.format(Locale.getDefault(), date, count);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static boolean isMinutesDiffMax(DateTime dateTime1, DateTime dateTime2, int maxMinutes) {
        Period period = new Period(dateTime1, dateTime2);
        Duration duration = period.toDurationFrom(dateTime1);
        return Math.abs(duration.toStandardMinutes().getMinutes()) <= maxMinutes;
    }

    public static boolean wasMinutesAgoMax(Date date, int maxMinutes) {
        DateTime now = new DateTime(DateTimeZone.getDefault());
        DateTime dateTime = new DateTime(date, DateTimeZone.getDefault());
        return isMinutesDiffMax(dateTime, now, maxMinutes);
    }

}

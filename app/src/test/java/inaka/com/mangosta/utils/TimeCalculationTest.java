package inaka.com.mangosta.utils;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;

public class TimeCalculationTest {

    @Test
    public void checkMinutesDiffMax() throws Exception {
        DateTime referenceDateTime = getDateTime(2016, 10, 26, 15, 20, 34);

        // check same date
        DateTime dateTime1 = getDateTime(2016, 10, 26, 15, 20, 34);
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime1, 0));

        DateTime dateTime2 = getDateTime(2016, 10, 26, 15, 21, 34);
        DateTime dateTime3 = getDateTime(2016, 10, 26, 15, 19, 34);
        DateTime dateTime4 = getDateTime(2016, 10, 26, 15, 20, 20);
        DateTime dateTime5 = getDateTime(3016, 10, 26, 15, 20, 34);
        DateTime dateTime6 = getDateTime(2016, 10, 30, 15, 20, 34);
        DateTime dateTime7 = getDateTime(2016, 10, 26, 15, 40, 34);
        DateTime dateTime8 = getDateTime(2016, 10, 26, 9, 20, 34);

        // true cases
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime2, 1));
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime2, 2));
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime3, 1));
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime4, 0));
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime7, 20));
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime7, 30));
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime8, 360));

        // false cases
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime1, -1));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime5, 5000));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime6, 500));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime7, 19));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime7, 0));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(referenceDateTime, dateTime8, 350));
    }

    private static DateTime getDateTime(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, 0);
        return new DateTime(cal.getTime(), DateTimeZone.getDefault());
    }

}

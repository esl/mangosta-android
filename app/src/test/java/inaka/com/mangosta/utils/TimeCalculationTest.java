package inaka.com.mangosta.utils;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;

public class TimeCalculationTest {

    private DateTime mReferenceDateTime;

    @Before
    public void initReferenceDateTime() {
        mReferenceDateTime = getDateTime(2016, 10, 26, 15, 20, 34);
    }

    @Test
    public void checkMinutesDiffMaxWithSameDate() throws Exception {
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, mReferenceDateTime, 0));
    }

    @Test
    public void checkMinutesDiffMaxWithNegativeInput() throws Exception {
        DateTime dateTime1 = getDateTime(2016, 10, 26, 15, 20, 55);
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime1, -1));

        DateTime dateTime2 = getDateTime(2016, 10, 26, 15, 22, 55);
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime2, -24));
    }

    @Test
    public void checkMinutesDiffMaxWithSecondsDifference() {
        DateTime dateTime1 = getDateTime(2016, 10, 26, 15, 20, 20);
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime1, 0));

        DateTime dateTime2 = getDateTime(2016, 10, 26, 15, 21, 33);
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime2, 0));
    }

    @Test
    public void checkMinutesDiffMaxWithMinutesDifference() {
        DateTime dateTime1 = getDateTime(2016, 10, 26, 15, 21, 34);
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime1, 1));
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime1, 2));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime1, 0));

        DateTime dateTime2 = getDateTime(2016, 10, 26, 15, 19, 34);
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime2, 1));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime2, 0));

        DateTime dateTime3 = getDateTime(2016, 10, 26, 15, 25, 34);
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime3, 10));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime3, 4));

        DateTime dateTime4 = getDateTime(2016, 10, 26, 15, 40, 34);
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime4, 20));
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime4, 30));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime4, 19));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime4, 11));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime4, 0));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime4, -20));
    }

    @Test
    public void checkMinutesDiffMaxWithDaysAndMonthsDifference() {
        DateTime dateTime1 = getDateTime(2016, 10, 30, 15, 20, 34);
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime1, 500));

        DateTime dateTime2 = getDateTime(2016, 10, 26, 9, 20, 34);
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime2, 360));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime2, 350));

        DateTime dateTime3 = getDateTime(2016, 11, 26, 9, 20, 34);
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime3, 45000));
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime3, 8000));
    }

    @Test
    public void checkMinutesDiffMaxWithYearsDifference() {
        DateTime dateTime1 = getDateTime(3016, 10, 26, 15, 20, 34);
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime1, 5000));

        DateTime dateTime2 = getDateTime(2017, 11, 16, 15, 20, 34);
        Assert.assertFalse(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime2, 5000));
        Assert.assertTrue(TimeCalculation.isMinutesDiffMax(mReferenceDateTime, dateTime2, 10000000));
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

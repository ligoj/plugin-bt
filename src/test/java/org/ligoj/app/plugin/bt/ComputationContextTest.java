/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.plugin.bt.model.BusinessHours;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;

/**
 * Test class of {@link ComputationContext}
 * 
 * @author Fabrice Daugan
 */
public class ComputationContextTest extends AbstractDataGeneratorTest {

	@Test
	public void reset() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), new ArrayList<>());
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
	}

	@Test
	public void resetSaturday() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), new ArrayList<>());
		context.reset(getDate(2014, 03, 01));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
	}

	@Test
	public void resetSunday() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), new ArrayList<>());
		context.reset(getDate(2014, 03, 02));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
	}

	@Test
	public void moveForwardSame() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), new ArrayList<>());
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(0, context.moveForward(getDate(2014, 03, 03)));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
	}

	@Test
	public void moveForwardPlusOne() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), new ArrayList<>());
		context.reset(getDate(2014, 03, 03));
		final Date end = new Date(getDate(2014, 03, 03).getTime() + 1);
		Assertions.assertEquals(1, context.moveForward(end));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
		Assertions.assertEquals(1, context.getCursorTime());
	}

	@Test
	public void moveForwardMidnightLessOne() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), new ArrayList<>());
		context.reset(getDate(2014, 03, 03));
		final Date end = new Date(getDate(2014, 03, 04).getTime() - 1);
		Assertions.assertEquals(DateUtils.MILLIS_PER_DAY - 1, context.moveForward(end));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
		Assertions.assertEquals(DateUtils.MILLIS_PER_DAY - 1, context.getCursorTime());
	}

	@Test
	public void moveForwardPlusOneWeek() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), new ArrayList<>());
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(5 * DateUtils.MILLIS_PER_DAY, context.moveForward(getDate(2014, 03, 10)));
		Assertions.assertEquals(getDate(2014, 03, 10), context.getCursor());
	}

	@Test
	public void moveForwardPlusOnePartialWeek() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), new ArrayList<>());
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(5 * DateUtils.MILLIS_PER_DAY, context.moveForward(getDate(2014, 03, 8)));
		Assertions.assertEquals(getDate(2014, 03, 10), context.getCursor());
	}

	@Test
	public void resetHoliday() {
		final List<Date> holidays = new ArrayList<>();
		holidays.add(getDate(2014, 03, 01));
		holidays.add(getDate(2014, 03, 03));
		holidays.add(getDate(2014, 03, 05));
		final ComputationContext context = new ComputationContext(holidays, new ArrayList<>());
		context.reset(getDate(2014, 03, 01));
		Assertions.assertEquals(getDate(2014, 03, 04), context.getCursor());
	}

	@Test
	public void moveForwardOnWeekHoliday() {
		final List<Date> holidays = new ArrayList<>();
		holidays.add(getDate(2014, 03, 04));
		holidays.add(getDate(2014, 03, 06));
		holidays.add(getDate(2014, 03, 10));
		final ComputationContext context = new ComputationContext(holidays, new ArrayList<>());
		context.reset(getDate(2014, 03, 01));
		// 2014/03/01 = Sat, 2014/03/02 = Sun, 2014/03/08 = Sat, 2014/03/09 =
		// Sun
		Assertions.assertEquals(3 * DateUtils.MILLIS_PER_DAY, context.moveForward(getDate(2014, 03, 8)));
		Assertions.assertEquals(getDate(2014, 03, 11), context.getCursor());
	}

	@Test
	public void resetBusinessHoursFromNonBusinessDayOfWeek() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(9, 12, 14, 18));
		// 2014/03/01 = Sat, 2014/03/02 = Sun
		context.reset(getDate(2014, 03, 01));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
		Assertions.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void resetBusinessHoursFromEODNonBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 03, 19, 00, 00));
		Assertions.assertEquals(getDate(2014, 03, 04), context.getCursor());
		Assertions.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void resetBusinessHoursFromNonBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 03, 13, 59, 59));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
		Assertions.assertEquals(14 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void resetBusinessHoursFromBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 03, 15, 00, 00));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
		Assertions.assertEquals(15 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void resetHolidayBusinessHours() {
		final List<Date> holidays = new ArrayList<>();
		holidays.add(getDate(2014, 03, 03));
		final ComputationContext context = new ComputationContext(holidays, newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 01, 19, 00, 00));
		Assertions.assertEquals(getDate(2014, 03, 04), context.getCursor());
		Assertions.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardToSameDayBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(0, context.moveForward(getDate(2014, 03, 03)));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
		Assertions.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardDurationToSameDayBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(getDate(2014, 03, 03, 11, 0, 0), context.moveForward(DateUtils.MILLIS_PER_HOUR * 2));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
		Assertions.assertEquals(11 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardToNextDayBusinessHourMidnight() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(7 * DateUtils.MILLIS_PER_HOUR, context.moveForward(getDate(2014, 03, 04)));
		Assertions.assertEquals(getDate(2014, 03, 04), context.getCursor());
		Assertions.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardDurationToNextDayBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(getDate(2014, 03, 04, 9, 0, 0), context.moveForward(DateUtils.MILLIS_PER_HOUR * 7));
		Assertions.assertEquals(getDate(2014, 03, 04), context.getCursor());
		Assertions.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardToNextDayNonBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(7 * DateUtils.MILLIS_PER_HOUR, context.moveForward(getDate(2014, 03, 04, 8, 0, 0)));
		Assertions.assertEquals(getDate(2014, 03, 04), context.getCursor());
		Assertions.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardToNext2DayNonBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(7 * 2 * DateUtils.MILLIS_PER_HOUR, context.moveForward(getDate(2014, 03, 05, 8, 0, 0)));
		Assertions.assertEquals(getDate(2014, 03, 05), context.getCursor());
		Assertions.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardDurationToNext2DayNonBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(getDate(2014, 03, 05, 9, 0, 0), context.moveForward(7 * 2 * DateUtils.MILLIS_PER_HOUR));
		Assertions.assertEquals(getDate(2014, 03, 05), context.getCursor());
		Assertions.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardToNextDayBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(11 * DateUtils.MILLIS_PER_HOUR, context.moveForward(getDate(2014, 03, 04, 15, 0, 0)));
		Assertions.assertEquals(getDate(2014, 03, 04), context.getCursor());
		Assertions.assertEquals(15 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardPerformance() {
		final List<Date> holidays = new ArrayList<>();
		holidays.add(getDate(2014, 03, 04));
		holidays.add(getDate(2014, 03, 06));
		for (int year = 2014; year < 2050; year++) {
			holidays.add(getDate(year, 03, 10));
		}
		Assertions.assertTimeout(Duration.ofSeconds(2), () -> {
			final ComputationContext context = new ComputationContext(holidays, newRanges(9, 12, 14, 18));
			Date lastDate = getDate(2014, 03, 03);
			final List<Date> dates = new ArrayList<>();
			context.reset(lastDate);
			final Random random = new Random(1);
			for (int i = 10000; i-- > 0;) {
				final long increment = 2500 + (long) ((2 * DateUtils.MILLIS_PER_DAY - 50) * random.nextDouble());
				final Date newDate = new Date(lastDate.getTime() + increment);
				dates.add(newDate);
				lastDate = newDate;
			}
			long delta = 0;
			for (final Date date : dates) {
				delta += context.moveForward(date);
			}
			Assertions.assertEquals(180633600000L, delta);
		});
	}

	@Test
	public void resetNightBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(0, 10, 22, 24));
		context.reset(getDate(2014, 03, 03, 23, 00, 00));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
		Assertions.assertEquals(23 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
		context.reset(getDate(2014, 03, 03, 8, 00, 00));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
		Assertions.assertEquals(8 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void resetNightBusinessHourFromNotBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(0, 10, 22, 24));
		context.reset(getDate(2014, 03, 03, 10, 00, 00));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
		Assertions.assertEquals(22 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void resetNightBusinessHourFromMidnight() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(0, 10, 22, 24));
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
		Assertions.assertEquals(0, context.getCursorTime());
	}

	@Test
	public void moveForwardNightBusinessHourSameDayNonBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(0, 10, 22, 24));
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(10 * DateUtils.MILLIS_PER_HOUR, context.moveForward(getDate(2014, 03, 03, 15, 0, 0)));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
		Assertions.assertEquals(22 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardNightBusinessHourSameDay() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(0, 10, 22, 24));
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(11 * DateUtils.MILLIS_PER_HOUR, context.moveForward(getDate(2014, 03, 03, 23, 0, 0)));
		Assertions.assertEquals(getDate(2014, 03, 03), context.getCursor());
		Assertions.assertEquals(23 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardNightBusinessHourNextDay() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(0, 10, 12, 14, 22, 24));
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(26 * DateUtils.MILLIS_PER_HOUR, context.moveForward(getDate(2014, 03, 04, 15, 0, 0)));
		Assertions.assertEquals(getDate(2014, 03, 04), context.getCursor());
		Assertions.assertEquals(22 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardNightBusinessHourNextWeek() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(0, 10, 22, 23));
		context.reset(getDate(2014, 03, 03, 7, 0, 0));

		// 2014/03/03 07:00:00 -> 2014/03/03 10:00:00 -> 3:00:00
		// 2014/03/03 22:00:00 -> 2014/03/03 23:00:00 -> 1:00:00
		// 2014/03/04 00:00:00 -> 2014/03/04 10:00:00 -> 10:00:00 x 5 (=
		// 10-4-1[2014/03/08 = Sat]-1[2014/03/09 = Sun})
		// 2014/03/04 22:00:00 -> 2014/03/04 23:00:00 -> 1:00:00 x 5 (=
		// 10-4-1[2014/03/08 = Sat]-1[2014/03/09 = Sun})
		Assertions.assertEquals(((3 + 1) + (5 * 11)) * DateUtils.MILLIS_PER_HOUR, context.moveForward(getDate(2014, 03, 10, 23, 30, 0)));
		Assertions.assertEquals(getDate(2014, 03, 11), context.getCursor());
		Assertions.assertEquals(0, context.getCursorTime());
	}

	@Test
	public void moveForwardToNext2DayBusinessHour() {
		final ComputationContext context = new ComputationContext(new ArrayList<>(), newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 03));
		Assertions.assertEquals(18 * DateUtils.MILLIS_PER_HOUR, context.moveForward(getDate(2014, 03, 05, 15, 0, 0)));
		Assertions.assertEquals(getDate(2014, 03, 05), context.getCursor());
		Assertions.assertEquals(15 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardInsideNonBusinessHoursAndDays() {
		final List<Date> holidays = new ArrayList<>();
		holidays.add(getDate(2014, 03, 06));
		holidays.add(getDate(2014, 03, 07));
		final ComputationContext context = new ComputationContext(holidays, newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 05, 19, 0, 0));
		Assertions.assertEquals(0, context.moveForward(getDate(2014, 03, 8, 8, 0, 0)));

		// 2014/03/09 is Sunday --> 2014/03/10
		Assertions.assertEquals(getDate(2014, 03, 10), context.getCursor());
		Assertions.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardInsideNonBusinessHoursAndDaysHolidayAfterEOW() {
		final List<Date> holidays = new ArrayList<>();
		holidays.add(getDate(2014, 03, 06));
		holidays.add(getDate(2014, 03, 07));
		holidays.add(getDate(2014, 03, 10));
		final ComputationContext context = new ComputationContext(holidays, newRanges(9, 12, 14, 18));
		context.reset(getDate(2014, 03, 05, 19, 0, 0));
		Assertions.assertEquals(0, context.moveForward(getDate(2014, 03, 8, 8, 0, 0)));

		// 2014/03/09 is Sunday --> 2014/03/10
		// 2014/03/10 is Holiday --> 2014/03/11
		Assertions.assertEquals(getDate(2014, 03, 11), context.getCursor());
		Assertions.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardInsideNonBusinessHoursAndDaysStickyRanges() {
		final List<Date> holidays = new ArrayList<>();
		holidays.add(getDate(2014, 03, 06));
		holidays.add(getDate(2014, 03, 07));
		final ComputationContext context = new ComputationContext(holidays, newRanges(9, 12, 14, 16, 16, 18));
		context.reset(getDate(2014, 03, 05, 17, 59, 59));
		Assertions.assertEquals(DateUtils.MILLIS_PER_SECOND, context.moveForward(getDate(2014, 03, 8, 8, 0, 0)));

		// 2014/03/09 is Sunday --> 2014/03/10
		Assertions.assertEquals(getDate(2014, 03, 10), context.getCursor());
		Assertions.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	@Test
	public void moveForwardInsideNonBusinessHoursAndDaysStickyRanges2() {
		final List<Date> holidays = new ArrayList<>();
		holidays.add(getDate(2014, 03, 06));
		holidays.add(getDate(2014, 03, 07));
		final ComputationContext context = new ComputationContext(holidays, newRanges(9, 12, 14, 18, 24, 24));
		context.reset(getDate(2014, 03, 05, 17, 59, 59));
		Assertions.assertEquals(DateUtils.MILLIS_PER_SECOND, context.moveForward(getDate(2014, 03, 8, 8, 0, 0)));

		// 2014/03/09 is Sunday --> 2014/03/10
		Assertions.assertEquals(getDate(2014, 03, 10), context.getCursor());
		Assertions.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, context.getCursorTime());
	}

	/**
	 * Return a list of business hours ranges.
	 */
	private List<BusinessHours> newRanges(final int... businessHoursCouples) {
		final List<BusinessHours> ranges = new ArrayList<>();
		for (int i = 0; i < businessHoursCouples.length; i += 2) {
			final BusinessHours range = new BusinessHours();
			range.setStart(businessHoursCouples[i] * DateUtils.MILLIS_PER_HOUR);
			range.setEnd(businessHoursCouples[i + 1] * DateUtils.MILLIS_PER_HOUR);
			ranges.add(range);
		}
		return ranges;
	}
}

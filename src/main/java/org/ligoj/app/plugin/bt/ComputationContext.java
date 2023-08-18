/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.ligoj.app.plugin.bt.model.BusinessHours;

import lombok.Getter;

/**
 * A computation context for a fixed start and end dates.
 *
 * @author Fabrice Daugan
 */
public class ComputationContext {

	/**
	 * The cursor date only, at 0:00 00.000. Truncated to {@link Calendar#DATE}
	 */
	@Getter
	private Date cursor;

	/**
	 * The non-business days. Each day must be set to start of the day position.
	 */
	private final List<Date> holidays;

	/**
	 * The non-business hour ranges. Never empty or must be sorted.
	 */
	private final List<BusinessHours> businessHours;

	/**
	 * Last read holiday index within {@link #holidays}
	 */
	private int holidayCursor;

	/**
	 * Current computed delta value.
	 */
	private long delta;

	/**
	 * Time cursor with a day. Between 0 and {@link DateUtils#MILLIS_PER_DAY}
	 */
	@Getter
	private long cursorTime;

	/**
	 * The current index of time range within {@link #businessHours}..
	 */
	private int cursorBusinessHour;

	/**
	 * initialize the computation context.
	 *
	 * @param holidays
	 *            the holiday list. Each day must be set to start of the day position.
	 * @param businessHours
	 *            The business hour ranges. May be empty or must be sorted, and first range must start with 0:00 00.000.
	 */
	public ComputationContext(final List<Date> holidays, final List<BusinessHours> businessHours) {
		this.holidays = holidays;
		if (businessHours.isEmpty()) {
			// Whole day is a working day
			final BusinessHours businessHour = new BusinessHours();
			businessHour.setStart(0);
			businessHour.setEnd(DateUtils.MILLIS_PER_DAY);
			this.businessHours = Collections.singletonList(businessHour);
		} else {
			this.businessHours = businessHours;
		}
	}

	/**
	 * Set the initial date. This date will be moved forward until to find the closest opening day and the business
	 * hour.
	 *
	 * @param start
	 *            the initial date.
	 */
	public void reset(final Date start) {
		this.cursor = new Date(DateUtils.truncate(start, Calendar.DATE).getTime());
		this.holidayCursor = 0;
		this.cursorTime = getTime(start);
		moveToNextBusiness();
	}

	/**
	 * Move the cursors to next valid business days, business hours and time. No updated delta.
	 */
	public void moveToNextBusiness() {
		moveToNextBusinessDay();
		moveToNextBusinessHour();
	}

	/**
	 * Move the cursors to the next business hour. No updated delta.
	 */
	private void moveToNextBusinessHour() {
		for (cursorBusinessHour = 0; cursorBusinessHour < businessHours.size(); cursorBusinessHour++) {
			final BusinessHours range = businessHours.get(cursorBusinessHour);
			if (cursorTime <= range.getStart()) {
				// The current range is the closest business hours range
				cursorTime = range.getStart();
				return;
			}
			if (cursorTime < range.getEnd()) {
				// The current range is the right business hours range
				return;
			}
		}

		// End of current period, need to move to next day and business range again
		moveToTomorrow();
	}

	/**
	 * Return the next business day : business day of week and not a holiday. No updated delta.
	 */
	private void moveToNextBusinessDay() {
		Date previousDate;
		do {
			previousDate = cursor;
			moveToNextBusinessDayOfWeek();
			moveToNextNotHoliday();
		} while (!cursor.equals(previousDate));
	}

	/**
	 * Move the date to the next day that is not a holiday. No updated delta.
	 */
	private void moveToNextNotHoliday() {
		while (holidayCursor < holidays.size()) {
			final Date holiday = holidays.get(holidayCursor);
			if (holiday.getTime() > cursor.getTime()) {
				// The date is before the first known holiday
				break;
			}

			// Either it's a holiday, either we have to skip increase cursor
			holidayCursor++;
			if (DateUtils.isSameDay(holiday, cursor)) {
				// The date is a holiday, go to the next day 00:00 00.000
				moveToTomorrow();
			}
			// The holiday cursor is too old, advance it only
		}

		// All holidays are before this date
	}

	/**
	 * Move the date to the next business day of week. No updated delta.
	 */
	private void moveToNextBusinessDayOfWeek() {
		final int dow = DateUtils.toCalendar(cursor).get(Calendar.DAY_OF_WEEK);
		if (dow == Calendar.SUNDAY || dow == Calendar.SATURDAY) {
			// Sunday/Saturday --> Monday 00:00 00.000
			moveToTomorrow();
		}
	}

	/**
	 * Move the cursors to tomorrow, then the next business time. No updated delta.
	 */
	private void moveToTomorrow() {
		cursor = DateUtils.ceiling(cursor, Calendar.DATE);
		cursorBusinessHour = 0;
		cursorTime = businessHours.get(0).getStart();
		moveToNextBusiness();
	}

	/**
	 * Compute time duration in milliseconds between last known (or initial date) and the given date.
	 *
	 * @param end
	 *            The end date for delta computation. This date should be after the last known one or will return
	 *            <code>0</code>.
	 * @return time duration in milliseconds between start and end date. The returned value is a positive number.
	 */
	public long moveForward(final Date end) {
		this.delta = 0;
		while (cursor.getTime() + cursorTime < end.getTime()) {
			// We need to move the cursors
			if (DateUtils.isSameDay(cursor, end)) {
				// We need to compute the elapsed ranges and hours within the same day
				computeDelayTodayToTime(getTime(end));
			} else {
				// Move to the end of this day
				computeDelayTodayToTime(DateUtils.MILLIS_PER_DAY);
			}
		}
		return delta;
	}

	/**
	 * Advance the cursor with the given duration.
	 *
	 * @param duration
	 *            Duration to add to current date. Business hours are considered.
	 * @return The new date.
	 */
	public Date moveForward(final long duration) {
		long remainingDuration = duration;
		while (remainingDuration > 0) {
			this.delta = 0;

			// We need to move the cursors
			if (cursorTime + remainingDuration < DateUtils.MILLIS_PER_DAY) {
				// We need to compute the elapsed ranges and hours within the same day
				computeDelayTodayToTime(cursorTime + remainingDuration);
			} else {
				// Move to the end of this day
				computeDelayTodayToTime(DateUtils.MILLIS_PER_DAY);
			}
			remainingDuration -= delta;
		}

		// Return the new date
		return new Date(cursor.getTime() + getCursorTime());
	}

	/**
	 * Compute the elapsed time within the same day, and move forward to the starting day of closest next business hour.
	 * Midnight stills considered as same day since the end bound is excluded.
	 */
	private void computeDelayTodayToTime(final long time) {
		// Move from the position of current index of business hours range
		while (cursorBusinessHour < businessHours.size()) {
			final BusinessHours range = businessHours.get(cursorBusinessHour);
			if (cursorTime < range.getStart()) {
				// Update the time cursor to the start of this closest range, no updated delay
				cursorTime = range.getStart();
				return;
			}
			if (time < range.getEnd()) {
				// target time ends before the end of current business hours range
				delta += time - cursorTime;
				cursorTime = time;
				return;
			}

			// Current range is completed
			delta += range.getEnd() - cursorTime;
			cursorTime = range.getEnd();

			// Move to the next range
			cursorBusinessHour++;
		}

		// End of day reached, move to the next business day
		moveToTomorrow();
	}

	/**
	 * Return the milliseconds of given date. Year, month and day fields are ignored.
	 */
	private long getTime(final Date date) {
		final Calendar calendar = DateUtils.toCalendar(date);
		return calendar.get(Calendar.HOUR_OF_DAY) * DateUtils.MILLIS_PER_HOUR
				+ calendar.get(Calendar.MINUTE) * DateUtils.MILLIS_PER_MINUTE
				+ calendar.get(Calendar.SECOND) * DateUtils.MILLIS_PER_SECOND + calendar.get(Calendar.MILLISECOND);
	}
}

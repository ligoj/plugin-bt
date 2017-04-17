package org.ligoj.app.plugin.bt.dao;

import java.util.Date;
import java.util.List;

import org.ligoj.app.plugin.bt.model.Holiday;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link Holiday} repository.
 */
public interface HolidayRepository extends RestRepository<Holiday, Integer> {

	/**
	 * Return all non business date associated to the given project and between two date.
	 * 
	 * @param subscription
	 *            The subscription identifier.
	 * @param from
	 *            the farthest date to get.
	 * @param to
	 *            the nearest date to get.
	 * @return the ordered matching dates.
	 */
	@Query("SELECT h.date FROM Holiday h, BugTrackerConfiguration p WHERE h.calendar = p.calendar AND p.subscription.id = ?1 AND h.date >= ?2 AND h.date <= ?3 ORDER BY h.date")
	List<Date> getHolidays(int subscription, Date from, Date to);

}

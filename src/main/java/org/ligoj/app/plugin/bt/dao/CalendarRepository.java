/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.dao;

import org.ligoj.app.plugin.bt.model.Calendar;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link Calendar} repository.
 */
public interface CalendarRepository extends RestRepository<Calendar, Integer> {

	/**
	 * Return default configuration.
	 * 
	 * @return The default calendar.
	 */
	@Query("FROM Calendar WHERE asDefault=true")
	Calendar getDefault();
}

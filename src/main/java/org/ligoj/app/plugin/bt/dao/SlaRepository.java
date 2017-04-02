package org.ligoj.app.plugin.bt.dao;

import java.util.List;

import org.ligoj.app.plugin.bt.model.Sla;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link Sla} repository.
 */
public interface SlaRepository extends RestRepository<Sla, Integer> {

	/**
	 * Return {@link Sla} associated to the given project.
	 * 
	 * @param subscription
	 *            the subscription identifier.
	 * @return the {@link Sla} objects associated to the given project.
	 */
	@Query("FROM Sla s WHERE s.configuration.subscription.id = ?1 ORDER BY s.name ASC")
	List<Sla> findBySubscription(int subscription);

	/**
	 * Delete the SLA associated to the given subscription.
	 * 
	 * @param subscription
	 *            the subscription identifier.
	 */
	@Modifying
	@Query("DELETE Sla s WHERE s.configuration.id IN (SELECT bt.id FROM BugTrackerConfiguration bt WHERE bt.subscription.id = ?1)")
	void deleteBySubscription(int subscription);
}

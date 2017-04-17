package org.ligoj.app.plugin.bt.dao;

import org.ligoj.app.plugin.bt.model.BugTrackerConfiguration;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link BugTrackerConfiguration} repository.
 */
public interface BugTrackerConfigurationRepository extends RestRepository<BugTrackerConfiguration, Integer> {

	/**
	 * Return the {@link BugTrackerConfiguration} of given subscription, fetch
	 * calendar, business hours, subscription and project.
	 *
	 * @param subscription
	 *            the subscription identifier.
	 * @return the matching {@link BugTrackerConfiguration} object.
	 */
	@Query("FROM BugTrackerConfiguration bt INNER JOIN FETCH bt.subscription subscription INNER JOIN FETCH subscription.project project"
			+ " LEFT JOIN FETCH bt.calendar c LEFT JOIN FETCH bt.businessHours bh" + " WHERE subscription.id = ?1")
	BugTrackerConfiguration findBySubscriptionFetch(int subscription);

	/**
	 * Return the {@link BugTrackerConfiguration} of given subscription.
	 *
	 * @param subscription
	 *            the subscription identifier.
	 * @return the matching {@link BugTrackerConfiguration} object.
	 */
	@Query("FROM BugTrackerConfiguration WHERE subscription.id = ?1")
	BugTrackerConfiguration findBySubscription(int subscription);
}

package org.ligoj.app.plugin.bt;

import java.util.Set;

import org.ligoj.app.api.ServicePlugin;

/**
 * Features of bug tracker implementations.
 */
public interface BugTrackerServicePlugin extends ServicePlugin {

	/**
	 * Return available statuses normalized text.
	 * 
	 * @param subscription
	 *            the subscription's identifier.
	 * @return the statuses.
	 */
	Set<String> getStatuses(int subscription) throws Exception;

	/**
	 * Return available types text.
	 * 
	 * @param subscription
	 *            the subscription's identifier.
	 * @return the types.
	 */
	Set<String> getTypes(int subscription) throws Exception;

	/**
	 * Return available priorities text.
	 * 
	 * @param subscription
	 *            the subscription's identifier.
	 * @return the priorities.
	 */
	Set<String> getPriorities(int subscription) throws Exception;

	/**
	 * Return available resolutions text.
	 * 
	 * @param subscription
	 *            the subscription's identifier.
	 * @return the resolutions.
	 */
	Set<String> getResolutions(int subscription) throws Exception;
}

/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt;

import java.io.IOException;
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
	 * @throws IOException
	 *             When statuses cannot read.
	 */
	Set<String> getStatuses(int subscription) throws IOException;

	/**
	 * Return available types text.
	 * 
	 * @param subscription
	 *            the subscription's identifier.
	 * @return the types.
	 * @throws IOException
	 *             When types cannot read.
	 */
	Set<String> getTypes(int subscription) throws IOException;

	/**
	 * Return available priorities text.
	 * 
	 * @param subscription
	 *            the subscription's identifier.
	 * @return the priorities.
	 * @throws IOException
	 *             When priorities cannot read.
	 */
	Set<String> getPriorities(int subscription) throws IOException;

	/**
	 * Return available resolutions text.
	 * 
	 * @param subscription
	 *            the subscription's identifier.
	 * @return the resolutions.
	 * @throws IOException
	 *             When resolutions cannot read.
	 */
	Set<String> getResolutions(int subscription) throws IOException;
}

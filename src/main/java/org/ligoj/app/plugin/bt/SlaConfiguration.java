/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt;

import java.util.List;

import org.ligoj.bootstrap.core.DescribedBean;

import lombok.Getter;
import lombok.Setter;

/**
 * SLA Configuration
 */
@Getter
@Setter
public class SlaConfiguration extends DescribedBean<Integer> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Statuses to start this SLA.
	 */
	private List<String> start;

	/**
	 * Statuses to stop this SLA.
	 */
	private List<String> stop;

	/**
	 * Paused status names.
	 */
	private List<String> pause;

	/**
	 * Optional filtered types. When empty, no filter.
	 */
	private List<String> types;

	/**
	 * Optional filtered priorities. When empty, no filter.
	 */
	private List<String> priorities;

	/**
	 * Optional filtered resolutions. When empty, no filter.
	 */
	private List<String> resolutions;

	/**
	 * Threshold of this SLA : maximum millisecond to reach then {@link #stop} status. 0 means none.
	 */
	private long threshold;
}

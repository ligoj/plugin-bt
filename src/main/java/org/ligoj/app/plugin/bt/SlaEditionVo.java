/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt;

import java.util.List;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.ligoj.bootstrap.core.DescribedBean;

import lombok.Getter;
import lombok.Setter;

/**
 * SLA edition bean.
 */
@Getter
@Setter
public class SlaEditionVo extends DescribedBean<Integer> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	@Positive
	private int subscription;

	/**
	 * Status names to start this SLA.
	 */
	@Size(min = 1)
	private List<String> start;

	/**
	 * Status names to stop this SLA.
	 */
	@Size(min = 1)
	private List<String> stop;

	/**
	 * Paused status names.
	 */
	private List<String> pause;

	/**
	 * Priorities names to filter this SLA.
	 */
	private List<String> priorities;

	/**
	 * Resolutions names to filter this SLA.
	 */
	private List<String> resolutions;

	/**
	 * Type names to stop this SLA.
	 */
	private List<String> types;

	/**
	 * Threshold of this SLA : maximum millisecond to reach then {@link #stop} status. 0 means none.
	 */
	@PositiveOrZero
	private long threshold;

}

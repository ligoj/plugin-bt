/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * SLA computation for an SLA.
 */
@Getter
@Setter
public class SlaData {

	/**
	 * SLA duration, in milliseconds, from start to stop minus paused duration.
	 */
	private long duration;

	/**
	 * The revised due date, corresponds to the original due date where the paused duration has been added. May be
	 * <code>null</code>.
	 */
	private Date revisedDueDate;

	/**
	 * Distance between the stopped workflow time and the revised due date. May be <code>null</code> when the revised
	 * due date is not known.
	 */
	private Long revisedDueDateDistance;

	/**
	 * The first time the workflow started. May be <code>null</code>.
	 */
	private Date start;

	/**
	 * The first time the workflow stopped. May be <code>null</code>.
	 */
	private Date stop;
}

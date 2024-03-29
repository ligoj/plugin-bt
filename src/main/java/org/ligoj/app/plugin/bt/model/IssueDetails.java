/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.model;

import java.util.Date;

import org.ligoj.bootstrap.core.model.AbstractPersistable;

import lombok.Getter;
import lombok.Setter;

/**
 * An issue status details
 */
@Getter
@Setter
public class IssueDetails extends AbstractPersistable<Integer> {

	/**
	 * Issue key as visible for end user.
	 */
	private String pkey;

	/**
	 * Creation date of issue.
	 */
	private Date created;

	/**
	 * Issue priority.
	 */
	private Integer priority;

	/**
	 * Current status of issue
	 */
	private int status;

	/**
	 * Issue type.
	 */
	private int type;

	/**
	 * Resolution identifier.
	 */
	private Integer resolution;

	/**
	 * Reporter identifier
	 */
	private String reporter;

	/**
	 * Assignee
	 */
	private String assignee;

	/**
	 * Timing data
	 */
	private Integer timeSpent;
	private Integer timeEstimate;
	private Integer timeEstimateInit;

	/**
	 * Original due date. May be <code>null</code>.
	 */
	private Date dueDate;

}

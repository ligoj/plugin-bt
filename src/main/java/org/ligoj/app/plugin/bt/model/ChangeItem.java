package org.ligoj.app.plugin.bt.model;

import lombok.Getter;
import lombok.Setter;

/**
 * A issue status change
 */
@Getter
@Setter
public class ChangeItem extends IssueDetails {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Status from.
	 */
	private Integer fromStatus;

	/**
	 * Status to
	 */
	private int toStatus;

}

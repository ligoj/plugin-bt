/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.model;

import lombok.Getter;
import lombok.Setter;

/**
 * An issue status change
 */
@Getter
@Setter
public class ChangeItem extends IssueDetails {

	/**
	 * Status from.
	 */
	private Integer fromStatus;

	/**
	 * Status to
	 */
	private int toStatus;

}

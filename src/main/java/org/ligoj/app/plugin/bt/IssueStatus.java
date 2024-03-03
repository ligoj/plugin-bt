/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt;

import java.util.List;

import org.ligoj.app.plugin.bt.model.IssueDetails;

import lombok.Getter;
import lombok.Setter;

/**
 * An issue status change
 */
@Getter
@Setter
public class IssueStatus extends IssueDetails {

	/**
	 * Ordered status changes.
	 */
	private List<StatusChange> changes;

}

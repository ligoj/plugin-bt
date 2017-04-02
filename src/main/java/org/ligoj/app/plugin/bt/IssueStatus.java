package org.ligoj.app.plugin.bt;

import java.util.List;

import org.ligoj.app.plugin.bt.model.IssueDetails;

import lombok.Getter;
import lombok.Setter;

/**
 * A issue status change
 */
@Getter
@Setter
public class IssueStatus extends IssueDetails {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Ordered status changes.
	 */
	private List<StatusChange> changes;

}

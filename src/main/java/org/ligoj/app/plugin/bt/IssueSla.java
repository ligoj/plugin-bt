package org.ligoj.app.plugin.bt;

import java.util.List;
import java.util.Map;

import org.ligoj.app.plugin.bt.model.IssueDetails;

import lombok.Getter;
import lombok.Setter;

/**
 * A issue with SLA durations.
 */
@Getter
@Setter
public class IssueSla extends IssueDetails {
	
	/**
	 * Ordered SLA data.
	 */
	private List<SlaData> data;
	
	/**
	 * Statuses counter.Key is the status identifier.
	 */
	private Map<Integer, Integer> statusCounter;
}

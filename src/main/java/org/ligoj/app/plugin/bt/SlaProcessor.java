/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.ligoj.app.plugin.bt.model.BusinessHours;
import org.ligoj.app.plugin.bt.model.ChangeItem;
import org.ligoj.app.plugin.bt.model.Sla;
import org.ligoj.bootstrap.core.DateUtils;
import org.ligoj.bootstrap.core.DescribedBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Compute SLA data.
 */
@Component
@Slf4j
public class SlaProcessor {

	@Autowired
	protected IdentifierHelper identifierHelper;

	/**
	 * Return SLA computations.
	 *
	 * @param businessHours The business hours.
	 * @param changes       the changes of all issues. Ordered by date.
	 * @param holidays      the non-business days.
	 * @param slas          the SLA configurations.
	 * @return the SLA configuration
	 */
	public SlaComputations process(final List<BusinessHours> businessHours, final List<ChangeItem> changes,
			final List<Date> holidays, final List<Sla> slas) {

		// Compute elapsed times
		final Map<Integer, IssueStatus> groupChanges = computedElapsedTimes(changes, holidays, businessHours);

		// Compute SLAs
		return computeSlas(groupChanges, slas, holidays, businessHours);
	}

	/**
	 * Compute SLA for each issue, based on the given status changes.
	 */
	private SlaComputations computeSlas(final Map<Integer, IssueStatus> groupChanges, final List<Sla> slas,
			final List<Date> holidays, final List<BusinessHours> nonBusinessHours) {
		final SlaComputations result = new SlaComputations();
		result.setSlaConfigurations(toSlaConfiguration(slas));
		result.setIssues(groupChanges.values().stream()
				.map(issue -> getIssueSlas(issue, slas, holidays, nonBusinessHours)).toList());
		return result;
	}

	/**
	 * Prepare SLA configuration to optimize the computations.
	 *
	 * @param slas the fresh SLA entities.
	 * @return the SLA configuration where status identifiers have been resolved to text. Paused statuses are ordered.
	 */
	public List<SlaConfiguration> toSlaConfiguration(final List<Sla> slas) {
		final List<SlaConfiguration> slaConfigurations = new ArrayList<>();
		for (final Sla sla : slas) {

			// Build the SLA configuration
			final SlaConfiguration configuration = new SlaConfiguration();
			configuration.setPause(identifierHelper.normalize(identifierHelper.asList(sla.getPause())));
			configuration.setStart(identifierHelper.normalize(identifierHelper.asList(sla.getStart())));
			configuration.setStop(identifierHelper.normalize(identifierHelper.asList(sla.getStop())));
			configuration.setPriorities(identifierHelper.asList(sla.getPriorities()));
			configuration.setResolutions(identifierHelper.asList(sla.getResolutions()));
			configuration.setTypes(identifierHelper.asList(sla.getTypes()));
			configuration.setThreshold(sla.getThreshold());
			DescribedBean.copy(sla, configuration);
			slaConfigurations.add(configuration);
		}

		return slaConfigurations;
	}

	/**
	 * Return the issue with all computed SLA.
	 */
	private IssueSla getIssueSlas(final IssueStatus issue, final List<Sla> slas, final List<Date> holidays,
			final List<BusinessHours> nonBusinessHours) {
		final IssueSla issueSla = new IssueSla();
		issueSla.setId(issue.getId());
		issueSla.setPriority(issue.getPriority());
		issueSla.setStatus(issue.getStatus());
		issueSla.setType(issue.getType());
		issueSla.setPkey(issue.getPkey());
		issueSla.setReporter(issue.getReporter());
		issueSla.setResolution(issue.getResolution());
		issueSla.setAssignee(issue.getAssignee());

		// Timing
		issueSla.setTimeSpent(issue.getTimeSpent());
		issueSla.setTimeEstimateInit(issue.getTimeEstimateInit());
		issueSla.setTimeEstimate(issue.getTimeEstimate());
		issueSla.setCreated(issue.getCreated());
		issueSla.setDueDate(issue.getDueDate());

		// Add elapsed times for all relevant statuses for each SLA
		issueSla.setData(getSlaDurations(issue, slas, holidays, nonBusinessHours));

		// Add status counter
		issueSla.setStatusCounter(getStatusCounter(issue));

		return issueSla;
	}

	/**
	 * For each change, increment the status counter.
	 */
	private Map<Integer, Integer> getStatusCounter(final IssueStatus issue) {
		final Map<Integer, Integer> statusCounter = new HashMap<>();
		for (final StatusChange change : issue.getChanges()) {
			// Increment the counter for this status
			final int status = change.getStatus();
			statusCounter.put(status, ObjectUtils.getIfNull(statusCounter.get(status), 0) + 1);
		}
		return statusCounter;
	}

	/**
	 * For each SLA, get the elapsed time for given issue.
	 */
	private List<SlaData> getSlaDurations(final IssueStatus issue, final List<Sla> slas, final List<Date> holidays,
			final List<BusinessHours> nonBusinessHours) {
		final List<SlaData> data = new ArrayList<>();
		final Date now = DateUtils.newCalendar().getTime();
		for (final Sla sla : slas) {
			if (checkAppliance(issue, sla)) {
				data.add(getSlaDuration(issue, sla, holidays, nonBusinessHours, now));
			} else {
				// Not applicable -> null
				data.add(null);
			}
		}
		return data;
	}

	/**
	 * Check the SLA can be applied for this issue.
	 *
	 * @param issue the current issue to check.
	 * @param sla   the SLA to compute.
	 * @return <code>true</code> if SLA can be applied to this issue.
	 */
	private boolean checkAppliance(final IssueStatus issue, final Sla sla) {
		return checkAppliance(issue.getType(), sla.getTypesAsSet())
				&& checkAppliance(issue.getPriority(), sla.getPrioritiesAsSet())
				&& checkAppliance(issue.getResolution(), sla.getResolutionsAsSet());
	}

	/**
	 * Check the value against the filtered ones.
	 *
	 * @param identifier          the current issue to check.
	 * @param filteredIdentifiers the filtered identifiers.
	 * @return <code>true</code> there is no filtered identifiers or when the given identifier is in the filtered
	 * identifiers.
	 */
	private boolean checkAppliance(final Integer identifier, final Set<Integer> filteredIdentifiers) {
		return filteredIdentifiers.isEmpty() || filteredIdentifiers.contains(identifier);
	}

	/**
	 * Return the elapsed time for the given SLA and issue or <code>null</code> if SLA cannot be applied for this issue.
	 */
	private SlaData getSlaDuration(final IssueStatus issue, final Sla sla, final List<Date> holidays,
			final List<BusinessHours> nonBusinessHours, final Date now) {
		boolean started = false;
		boolean paused = false;
		final ComputationContext computationContext = new ComputationContext(holidays, nonBusinessHours);
		final SlaData result = new SlaData();
		result.setRevisedDueDate(issue.getDueDate());

		for (final StatusChange change : issue.getChanges()) {
			// Look the SLA triggers
			if (sla.getStopAsSet().contains(change.getStatus())) {
				// SLA is completed
				started = false;
				paused = false;
				if (result.getStop() == null) {
					// First encounter of stopped workflow
					result.setStop(change.getChange().getCreated());
				}
			} else if (started && sla.getPausedAsSet().contains(change.getStatus())) {
				// Paused SLA, update the revised due date only if the pause is before the current revised due date
				paused = true;
				updatePause(result, change, computationContext);
			} else if (sla.getStartAsSet().contains(change.getStatus())) {
				// Add time of the current status of the not ended SLA
				started = true;
				paused = false;
				result.setDuration(result.getDuration() + change.getElapsedtime());
				if (result.getStart() == null) {
					// First encounter of started workflow
					result.setStart(change.getChange().getCreated());
				}
			} else if (paused) {
				// Non managed state, continue the pause time
				updatePause(result, change, computationContext);
			} else if (started) {
				// Non managed state, continue the timer
				result.setDuration(result.getDuration() + change.getElapsedtime());
			}
		}

		if (result.getRevisedDueDate() != null) {
			// Compute the distance between the revised due date and the stopped workflow date
			Date stop = result.getStop();
			if (stop == null) {
				// Workflow is not yet stopped, continue the timer
				stop = now;
			}

			if (stop.after(result.getRevisedDueDate())) {
				// The SLA is invalid, the workflow stopped after the revised due date --> negative distance
				resetRevisedDueDate(computationContext, result);
				result.setRevisedDueDateDistance(-computationContext.moveForward(stop));
			} else {
				// The SLA is valid, the workflow stopped before the revised due date --> positive distance
				computationContext.reset(stop);
				result.setRevisedDueDateDistance(-computationContext.moveForward(result.getRevisedDueDate()));
			}
		}

		return result;
	}

	/**
	 * Reset computation context to due date as needed.
	 */
	private void resetRevisedDueDate(final ComputationContext computationContext, final SlaData result) {
		if (computationContext.getCursor() == null) {
			// First shift
			computationContext.reset(result.getRevisedDueDate());
		}
	}

	/**
	 * Update the pause context.
	 */
	private void updatePause(final SlaData result, final StatusChange change,
			final ComputationContext computationContext) {
		if (result.getRevisedDueDate() != null && result.getRevisedDueDate().after(change.getChange().getCreated())) {
			resetRevisedDueDate(computationContext, result);

			// Update the revised due date
			result.setRevisedDueDate(computationContext.moveForward(change.getElapsedtime()));
		}
	}

	/**
	 * Compute elapsed time for each related issues.
	 */
	private Map<Integer, IssueStatus> computedElapsedTimes(final List<ChangeItem> changes, final List<Date> holidays,
			final List<BusinessHours> nonBusinessHours) {
		final Map<Integer, IssueStatus> groupChanges = new LinkedHashMap<>();
		if (changes.isEmpty()) {
			// Save some useless computations
			return groupChanges;
		}
		final ComputationContext computationContext = new ComputationContext(holidays, nonBusinessHours);
		computationContext.reset(changes.getFirst().getCreated());
		long cumulatedElapsed = computeElapsedTimes(changes, groupChanges, computationContext);

		// Add elapsed time until now
		cumulatedElapsed += computationContext.moveForward(new Date());

		// Update elapsed time for the tail
		for (final IssueStatus value : groupChanges.values()) {
			updatePreviousStatus(cumulatedElapsed, null, value);
		}
		return groupChanges;
	}

	/**
	 * Compute elapsed time for each related issues. Unfinished workflows are not computed there.
	 */
	private long computeElapsedTimes(final List<ChangeItem> changes, final Map<Integer, IssueStatus> groupChanges,
			final ComputationContext computationContext) {
		long cumulatedElapsed = 0;
		for (final ChangeItem change : changes) {

			// Compute the elapsed time for the last cursor to the creation of this change
			final long elapsed = computationContext.moveForward(change.getCreated());
			cumulatedElapsed += elapsed;
			final IssueStatus value = getIssueStatus(groupChanges, change);

			// Update the elapsed time in the previous status
			updatePreviousStatus(cumulatedElapsed, change, value);

			// Add the new status, even for a creation
			final StatusChange statusChange = new StatusChange();
			statusChange.setStatus(change.getToStatus());
			statusChange.setSnapshotTime(cumulatedElapsed);
			statusChange.setChange(change);
			value.getChanges().add(statusChange);
		}
		return cumulatedElapsed;
	}

	/**
	 * Update the elapsed time of previous status.
	 */
	private void updatePreviousStatus(final long cumulatedElapsed, final ChangeItem change, final IssueStatus value) {
		if (!value.getChanges().isEmpty()) {
			// Check previous state
			final StatusChange statusChange = value.getChanges().getLast();

			// Update the elapsed time of previous state
			updateElapsedTime(cumulatedElapsed, statusChange);

			// Check the flow, change==null in tail creation case
			if (change != null) {
				checkTransition(change, statusChange, value);
			}
		}
	}

	/**
	 * Validate the transition.
	 */
	private void checkTransition(final ChangeItem change, final StatusChange statusChange, final IssueStatus value) {
		if (change.getFromStatus() != statusChange.getStatus()) {
			if (value.getChanges().size() == 1) {
				// The initial state was not correct, fix it
				log.info("Initial state of issue {} ({}) has been updated: {} -> {}", value.getPkey(), change.getId(), statusChange.getStatus(), change.getFromStatus());
				statusChange.setStatus(change.getFromStatus());
			} else {
				// The initial state was not correct, fix it
				log.info("Broken state of issue {} ({}) has been updated: {} -> {} to match the transition {} -> {}",
						value.getPkey(), change.getId(), statusChange.getStatus(),
						change.getFromStatus(), change.getFromStatus(), change.getToStatus());
				statusChange.setStatus(change.getFromStatus());
			}
		}
	}

	private IssueStatus getIssueStatus(final Map<Integer, IssueStatus> groupChanges, final ChangeItem change) {
		IssueStatus value = groupChanges.get(change.getId());
		if (value == null) {
			// Issue creation case
			value = new IssueStatus();
			value.setChanges(new ArrayList<>());
			value.setCreated(change.getCreated());
			value.setPkey(change.getPkey());
			value.setId(change.getId());
			value.setPriority(change.getPriority());
			value.setStatus(change.getStatus());
			value.setType(change.getType());
			value.setAssignee(change.getAssignee());
			value.setResolution(change.getResolution());
			value.setReporter(change.getReporter());

			// Timing
			value.setTimeSpent(change.getTimeSpent());
			value.setTimeEstimateInit(change.getTimeEstimateInit());
			value.setTimeEstimate(change.getTimeEstimate());
			value.setDueDate(change.getDueDate());

			groupChanges.put(change.getId(), value);
		}
		return value;
	}

	/**
	 * Set the elapsed time for the given status change.
	 */
	private void updateElapsedTime(final long cumulatedElapsed, final StatusChange statusChange) {
		statusChange.setElapsedtime(cumulatedElapsed - statusChange.getSnapshotTime());
	}
}

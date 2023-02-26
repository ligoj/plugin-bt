/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.plugin.bt.model.ChangeItem;
import org.ligoj.app.plugin.bt.model.Sla;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;

/**
 * {@link SlaProcessor} test class.
 * Shared SLA configuration
 * <br>
 * Pause : 3, 5<br>
 * Start : 2, 4<br>
 * Stop : 6
 */
class SlaProcessorTest extends AbstractDataGeneratorTest {

	private final SlaProcessor processor = new SlaProcessor();

	@BeforeEach
	void setupAutoWired() {
		processor.identifierHelper = new IdentifierHelper();
	}

	@Test
	void processNoIssue() {
		final SlaComputations process = processor.process(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
				new ArrayList<>());
		Assertions.assertEquals(0, process.getIssues().size());
		Assertions.assertEquals(0, process.getSlaConfigurations().size());
	}

	@Test
	void processNoSla() {
		final List<ChangeItem> changes = new ArrayList<>();
		changes.add(newChangeItem(1, 2));
		changes.add(newChangeItem(2, 3));

		final SlaComputations process = processor.process(new ArrayList<>(), changes, new ArrayList<>(), new ArrayList<>());
		Assertions.assertEquals(1, process.getIssues().size());
		Assertions.assertEquals(0, process.getSlaConfigurations().size());
	}

	@Test
	void processInvalidChangesInitial() {
		final List<ChangeItem> changes = new ArrayList<>();
		changes.add(newChangeItem(1, 2));
		changes.add(newChangeItem(1, 3));

		final SlaComputations process = processor.process(new ArrayList<>(), changes, new ArrayList<>(), new ArrayList<>());
		Assertions.assertEquals(1, process.getIssues().size());
		Assertions.assertEquals(0, process.getSlaConfigurations().size());
	}

	@Test
	void processInvalidChanges() {
		final List<ChangeItem> changes = new ArrayList<>();
		changes.add(newChangeItem(1, 2));
		changes.add(newChangeItem(2, 3));
		changes.add(newChangeItem(1, 3));

		final SlaComputations process = processor.process(new ArrayList<>(), changes, new ArrayList<>(), new ArrayList<>());
		Assertions.assertEquals(1, process.getIssues().size());
		Assertions.assertEquals(0, process.getSlaConfigurations().size());
	}

	/**
	 * Simplest SLA.
	 */
	@Test
	void process() {
		final List<ChangeItem> changes = new ArrayList<>();
		changes.add(newChangeItem(1, 2, 0)); // Start [2s]
		changes.add(newChangeItem(2, 3, 1)); // Pause
		changes.add(newChangeItem(3, 4, 2)); // Restart [2s]
		changes.add(newChangeItem(4, 5, 3)); // Pause
		changes.add(newChangeItem(5, 6, 4)); // Stop
		changes.add(newChangeItem(6, 7, 5)); // Ignored

		final List<Sla> slas = newSla();
		final SlaComputations process = processor.process(new ArrayList<>(), changes, new ArrayList<>(), slas);
		Assertions.assertEquals(1, process.getIssues().size());
		Assertions.assertEquals(1, process.getSlaConfigurations().size());
		Assertions.assertEquals(1, process.getIssues().get(0).getData().size());
		Assertions.assertEquals(4000, process.getIssues().get(0).getData().get(0).getDuration());
	}

	/**
	 * The due date is after all changes and would be shifted with all paused durations.
	 */
	@Test
	void processRevisedDueDateFuture() {
		final List<ChangeItem> changes = new ArrayList<>();
		final Date dueDate = getDate(2014, 7, 21, 1, 0, 0);
		changes.add(newChangeItem(1, 3, 0, dueDate)); // Ignored pause since timer is not started
		changes.add(newChangeItem(3, 2, 1, dueDate)); // Start [2s]
		changes.add(newChangeItem(2, 3, 2, dueDate)); // Pause [pause 2s]
		changes.add(newChangeItem(3, 4, 3, dueDate)); // Restart [2s]
		changes.add(newChangeItem(4, 5, 4, dueDate)); // Pause [pause 2s]
		changes.add(newChangeItem(5, 7, 5, dueDate)); // Ignored, continue pause [pause 2s]
		changes.add(newChangeItem(7, 8, 6, dueDate)); // Ignored, continue pause [pause 2s]
		changes.add(newChangeItem(8, 3, 7, dueDate)); // Pause, same state [pause 2s]
		changes.add(newChangeItem(3, 4, 8, dueDate)); // Restart [2s]
		changes.add(newChangeItem(4, 7, 9, dueDate)); // Ignored, continue timer [2s]
		changes.add(newChangeItem(7, 6, 10, dueDate)); // Stopped
		changes.add(newChangeItem(6, 7, 11, dueDate)); // Ignored
		changes.add(newChangeItem(7, 5, 12, dueDate)); // Ignored pause since timer is not started

		final List<Sla> slas = newSla();
		final SlaComputations process = processor.process(new ArrayList<>(), changes, new ArrayList<>(), slas);
		Assertions.assertEquals(1, process.getIssues().size());
		Assertions.assertEquals(1, process.getSlaConfigurations().size());
		final IssueSla issueSla = process.getIssues().get(0);
		// {2=1, 3=3, 4=2, 5=2, 6=1, 7=3, 8=1}
		Assertions.assertEquals(7, issueSla.getStatusCounter().size());
		Assertions.assertEquals(1, issueSla.getStatusCounter().get(2).intValue());
		Assertions.assertEquals(3, issueSla.getStatusCounter().get(3).intValue());
		Assertions.assertEquals(2, issueSla.getStatusCounter().get(4).intValue());
		Assertions.assertEquals(2, issueSla.getStatusCounter().get(5).intValue());
		Assertions.assertEquals(1, issueSla.getStatusCounter().get(6).intValue());
		Assertions.assertEquals(3, issueSla.getStatusCounter().get(7).intValue());
		Assertions.assertEquals(1, issueSla.getStatusCounter().get(8).intValue());
		Assertions.assertEquals(1, issueSla.getData().size());
		final SlaData slaData = issueSla.getData().get(0);
		Assertions.assertEquals(8000, slaData.getDuration());
		Assertions.assertEquals(dueDate, issueSla.getDueDate());

		// Pause equals to 10 seconds
		Assertions.assertEquals(getDate(2014, 7, 21, 1, 0, 10), slaData.getRevisedDueDate());
		Assertions.assertEquals(getDate(2014, 7, 21, 0, 0, 2), slaData.getStart());
		Assertions.assertEquals(getDate(2014, 7, 21, 0, 0, 20), slaData.getStop());
		Assertions.assertEquals(-(DateUtils.MILLIS_PER_HOUR - 20 * DateUtils.MILLIS_PER_SECOND + 10 * DateUtils.MILLIS_PER_SECOND),
				slaData.getRevisedDueDateDistance().longValue());
	}

	/**
	 * The due date is after the end of a pause change and before all pause changes.
	 */
	@Test
	void processRevisedDueDatePast() {
		final List<ChangeItem> changes = new ArrayList<>();
		final Date dueDate = getDate(2014, 7, 21, 0, 0, 1);
		changes.add(newChangeItem(1, 2, 0, dueDate)); // Start
		changes.add(newChangeItem(2, 3, 1, dueDate)); // Pause [2s]
		changes.add(newChangeItem(3, 4, 2, dueDate)); // Restart
		changes.add(newChangeItem(4, 5, 3, dueDate)); // Pause [2s]
		changes.add(newChangeItem(5, 6, 4, dueDate)); // Stop
		changes.add(newChangeItem(6, 7, 5, dueDate)); // Ignored
		changes.add(newChangeItem(7, 6, 4, dueDate)); // Stop (ignored)

		final List<Sla> slas = newSla();
		final SlaComputations process = processor.process(new ArrayList<>(), changes, new ArrayList<>(), slas);
		Assertions.assertEquals(1, process.getIssues().size());
		Assertions.assertEquals(1, process.getSlaConfigurations().size());
		final IssueSla issueSla = process.getIssues().get(0);
		Assertions.assertEquals(1, issueSla.getData().size());
		final SlaData slaData = issueSla.getData().get(0);
		Assertions.assertEquals(4000, slaData.getDuration());
		Assertions.assertEquals(dueDate, issueSla.getDueDate());

		// Unchanged due date
		Assertions.assertEquals(getDate(2014, 7, 21, 0, 0, 1), slaData.getRevisedDueDate());

		Assertions.assertEquals(getDate(2014, 7, 21, 0, 0, 0), slaData.getStart());
		Assertions.assertEquals(getDate(2014, 7, 21, 0, 0, 8), slaData.getStop());
		Assertions.assertEquals(-7000, slaData.getRevisedDueDateDistance().longValue());
	}

	/**
	 * The due date is before the end of the first pause but after its start.
	 */
	@Test
	void processRevisedDueDateMiddle() {
		final List<ChangeItem> changes = new ArrayList<>();
		final Date dueDate = getDate(2014, 7, 21, 0, 0, 5);
		changes.add(newChangeItem(1, 3, 0, dueDate)); // Ignored pause since timer is not started
		changes.add(newChangeItem(3, 2, 1, dueDate)); // Start [2s]
		changes.add(newChangeItem(2, 3, 2, dueDate)); // Pause
		changes.add(newChangeItem(3, 4, 3, dueDate)); // Restart [2s]
		changes.add(newChangeItem(4, 5, 4, dueDate)); // Pause
		changes.add(newChangeItem(5, 7, 5, dueDate)); // Ignored, continue pause
		changes.add(newChangeItem(7, 8, 6, dueDate)); // Ignored, continue pause
		changes.add(newChangeItem(8, 3, 7, dueDate)); // Pause, same state
		changes.add(newChangeItem(3, 4, 8, dueDate)); // Restart [2s]
		changes.add(newChangeItem(4, 7, 9, dueDate)); // Ignored, continue timer [2s]
		changes.add(newChangeItem(7, 6, 10, dueDate)); // Stopped
		changes.add(newChangeItem(6, 7, 11, dueDate)); // Ignored
		changes.add(newChangeItem(7, 5, 12, dueDate)); // Ignored pause since timer is not started

		final List<Sla> slas = newSla();
		final SlaComputations process = processor.process(new ArrayList<>(), changes, new ArrayList<>(), slas);
		Assertions.assertEquals(1, process.getIssues().size());
		Assertions.assertEquals(1, process.getSlaConfigurations().size());
		final IssueSla issueSla = process.getIssues().get(0);
		Assertions.assertEquals(1, issueSla.getData().size());
		final SlaData slaData = issueSla.getData().get(0);
		Assertions.assertEquals(8000, slaData.getDuration());
		Assertions.assertEquals(dueDate, issueSla.getDueDate());

		// Due date shift : 4 seconds
		Assertions.assertEquals(getDate(2014, 7, 21, 0, 0, 7), slaData.getRevisedDueDate());
		Assertions.assertEquals(getDate(2014, 7, 21, 0, 0, 2), slaData.getStart());
		Assertions.assertEquals(getDate(2014, 7, 21, 0, 0, 20), slaData.getStop());
		Assertions.assertEquals(-(20 - 7) * DateUtils.MILLIS_PER_SECOND, slaData.getRevisedDueDateDistance().longValue());
	}

	/**
	 * The due date is before the end of the first pause but after its start.
	 */
	@Test
	void processRevisedDueDateNotStopped() {
		final List<ChangeItem> changes = new ArrayList<>();
		final Date dueDate = getDate(2014, 7, 21, 0, 0, 5);
		changes.add(newChangeItem(1, 3, 0, dueDate)); // Ignored pause since timer is not started
		changes.add(newChangeItem(3, 2, 1, dueDate)); // Start [2s]
		changes.add(newChangeItem(2, 3, 2, dueDate)); // Pause
		changes.add(newChangeItem(3, 4, 3, dueDate)); // Restart [2s]
		changes.add(newChangeItem(4, 5, 4, dueDate)); // Pause
		changes.add(newChangeItem(5, 7, 5, dueDate)); // Ignored, continue pause
		changes.add(newChangeItem(7, 8, 6, dueDate)); // Ignored, continue pause
		changes.add(newChangeItem(8, 3, 7, dueDate)); // Pause, same state
		changes.add(newChangeItem(3, 4, 8, dueDate)); // Restart [2s]
		changes.add(newChangeItem(4, 7, 9, dueDate)); // Ignored, continue timer [2s]

		final List<Sla> slas = newSla();
		final SlaComputations process = processor.process(new ArrayList<>(), changes, new ArrayList<>(), slas);
		Assertions.assertEquals(1, process.getIssues().size());
		Assertions.assertEquals(1, process.getSlaConfigurations().size());
		final IssueSla issueSla = process.getIssues().get(0);
		Assertions.assertEquals(1, issueSla.getData().size());
		final SlaData slaData = issueSla.getData().get(0);
		Assertions.assertTrue(slaData.getDuration() > 47800000000L);
		Assertions.assertEquals(dueDate, issueSla.getDueDate());

		// Due date shift : 4 seconds
		Assertions.assertEquals(getDate(2014, 7, 21, 0, 0, 7), slaData.getRevisedDueDate());
		Assertions.assertEquals(getDate(2014, 7, 21, 0, 0, 2), slaData.getStart());
		Assertions.assertNull(slaData.getStop());
		Assertions.assertTrue(slaData.getRevisedDueDateDistance() < -47800000000L);
	}

	/**
	 * New SLA configuration
	 * <br>
	 * Pause : 3, 5<br>
	 * Start : 2, 4<br>
	 * Stop : 6<br>
	 */
	private List<Sla> newSla() {
		final List<Sla> slas = new ArrayList<>();
		final Sla sla = new Sla();
		sla.setStart("Open,Answered");
		final Set<Integer> startSet = new HashSet<>();
		startSet.add(2);
		startSet.add(4);
		sla.setStartAsSet(startSet);
		sla.setStop("Closed");
		final Set<Integer> endSet = new HashSet<>();
		endSet.add(6);
		sla.setStopAsSet(endSet);
		sla.setPause("Accepted,Expected");
		final Set<Integer> ignoredAsSet = new HashSet<>();
		ignoredAsSet.add(3);
		ignoredAsSet.add(5);
		sla.setPausedAsSet(ignoredAsSet);
		sla.setPrioritiesAsSet(new HashSet<>());
		sla.setResolutionsAsSet(new HashSet<>());
		sla.setTypesAsSet(new HashSet<>());
		slas.add(sla);
		return slas;
	}

	// Add a status change
	private ChangeItem newChangeItem(final int from, final int to, final Date dueDate) {
		return newChangeItem(from, to, from, dueDate);
	}

	// Add a status change
	private ChangeItem newChangeItem(final int from, final int to, final int shift, final Date dueDate) {
		final ChangeItem changeItem = new ChangeItem();
		changeItem.setCreated(getDate(2014, 7, 21, 0, 0, shift * 2));
		changeItem.setFromStatus(from);
		changeItem.setToStatus(to);
		changeItem.setStatus(to);
		changeItem.setType(1);
		changeItem.setResolution(1);
		changeItem.setPriority(1);
		changeItem.setDueDate(dueDate);
		return changeItem;
	}

	// Add a status change
	private ChangeItem newChangeItem(final int from, final int to, final int shift) {
		return newChangeItem(from, to, shift, null);
	}

	// Add a status change
	private ChangeItem newChangeItem(final int from, final int to) {
		return newChangeItem(from, to, null);
	}

	/**
	 * Simple SLA with all filters provided.
	 */
	@Test
	void processFilters() {
		final List<ChangeItem> changes = new ArrayList<>();
		changes.add(newChangeItem(1, 3, 0)); // Ignored pause since timer is not started
		changes.add(newChangeItem(3, 2, 1)); // Start [2s]
		changes.add(newChangeItem(2, 3, 2)); // Pause
		changes.add(newChangeItem(3, 4, 3)); // Restart [2s]
		changes.add(newChangeItem(4, 5, 4)); // Pause
		changes.add(newChangeItem(5, 7, 5)); // Ignored, continue pause
		changes.add(newChangeItem(7, 8, 6)); // Ignored, continue pause
		changes.add(newChangeItem(8, 3, 7)); // Pause, same state
		changes.add(newChangeItem(3, 4, 8)); // Restart [2s]
		changes.add(newChangeItem(4, 7, 9)); // Ignored, continue timer [2s]
		changes.add(newChangeItem(7, 6, 10)); // Stopped
		changes.add(newChangeItem(6, 7, 11)); // Ignored
		changes.add(newChangeItem(7, 5, 12)); // Ignored pause since timer is not started

		final List<Sla> slas = newSla();
		final Sla sla = slas.get(0);
		sla.setPriorities("Minor");
		sla.getPrioritiesAsSet().add(1);
		sla.setResolutions("Fixed");
		sla.getResolutionsAsSet().add(1);
		sla.setTypes("Bug");
		sla.getTypesAsSet().add(1);

		final SlaComputations process = processor.process(new ArrayList<>(), changes, new ArrayList<>(), slas);
		Assertions.assertEquals(1, process.getIssues().size());
		Assertions.assertEquals(1, process.getSlaConfigurations().size());
		Assertions.assertEquals(1, process.getIssues().get(0).getData().size());
		Assertions.assertEquals(8000, process.getIssues().get(0).getData().get(0).getDuration());
	}

	@Test
	void processType() {
		final List<ChangeItem> changes = new ArrayList<>();
		changes.add(newChangeItem(1, 2));

		final List<Sla> slas = newSla();
		final Sla sla = slas.get(0);
		sla.setTypes("Question");
		sla.getTypesAsSet().add(2);

		final SlaComputations process = processor.process(new ArrayList<>(), changes, new ArrayList<>(), slas);
		Assertions.assertEquals(1, process.getIssues().size());
		Assertions.assertEquals(1, process.getSlaConfigurations().size());
		Assertions.assertEquals(1, process.getIssues().get(0).getData().size());
		Assertions.assertNull(process.getIssues().get(0).getData().get(0));
	}

	@Test
	void processPriority() {
		final List<ChangeItem> changes = new ArrayList<>();
		changes.add(newChangeItem(1, 2));

		final List<Sla> slas = newSla();
		final Sla sla = slas.get(0);
		sla.setPriorities("Trivial");
		sla.getPrioritiesAsSet().add(2);

		final SlaComputations process = processor.process(new ArrayList<>(), changes, new ArrayList<>(), slas);
		Assertions.assertEquals(1, process.getIssues().size());
		Assertions.assertEquals(1, process.getSlaConfigurations().size());
		Assertions.assertEquals(1, process.getIssues().get(0).getData().size());
		Assertions.assertNull(process.getIssues().get(0).getData().get(0));
	}

	@Test
	void processResolution() {
		final List<ChangeItem> changes = new ArrayList<>();
		changes.add(newChangeItem(1, 2));

		final List<Sla> slas = newSla();
		final Sla sla = slas.get(0);
		sla.setResolutions("Won't Fix");
		sla.getResolutionsAsSet().add(2);

		final SlaComputations process = processor.process(new ArrayList<>(), changes, new ArrayList<>(), slas);
		Assertions.assertEquals(1, process.getIssues().size());
		Assertions.assertEquals(1, process.getSlaConfigurations().size());
		Assertions.assertEquals(1, process.getIssues().get(0).getData().size());
		Assertions.assertNull(process.getIssues().get(0).getData().get(0));
	}
}

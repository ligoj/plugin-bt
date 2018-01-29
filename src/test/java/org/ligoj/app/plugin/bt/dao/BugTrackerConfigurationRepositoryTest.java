package org.ligoj.app.plugin.bt.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.plugin.bt.BugTrackerResource;
import org.ligoj.app.plugin.bt.model.BugTrackerConfiguration;
import org.ligoj.app.plugin.bt.model.BusinessHours;
import org.ligoj.app.plugin.bt.model.Calendar;
import org.ligoj.app.plugin.bt.model.Holiday;
import org.ligoj.app.plugin.bt.model.Sla;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * {@link BugTrackerConfigurationRepository} test.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class BugTrackerConfigurationRepositoryTest extends AbstractAppTest {

	@Autowired
	private BugTrackerConfigurationRepository repository;

	@Autowired
	private SlaRepository slaRepository;

	@Autowired
	private HolidayRepository holidayRepository;

	@BeforeEach
	public void prepareData() throws IOException {
		persistEntities("csv", new Class[] { Calendar.class, Holiday.class, Node.class, Project.class,
				Subscription.class, BugTrackerConfiguration.class, BusinessHours.class, Sla.class },
				StandardCharsets.UTF_8.name());
	}

	@Test
	public void testOnlyForCoverageJpa() {
		Assertions.assertNotNull(slaRepository.findAll().iterator().next().getConfiguration());
		Assertions.assertNotNull(holidayRepository.findAll().iterator().next().getCalendar());
		new Calendar().setHolidays(Collections.emptyList());
		new BugTrackerConfiguration().setBusinessHours(Collections.emptyList());
		new BugTrackerConfiguration().setSlas(Collections.emptyList());
	}

	@Test
	public void findBySubscriptionFetch() {
		final int subscription = em
				.createQuery(
						"SELECT s.id FROM Subscription s WHERE s.project.name = ?1 AND s.node.id LIKE CONCAT(?2,'%')",
						Integer.class)
				.setParameter(1, "MDA").setParameter(2, BugTrackerResource.SERVICE_KEY).getSingleResult();
		final BugTrackerConfiguration bt = repository.findBySubscriptionFetch(subscription);

		// Project properties
		Assertions.assertEquals("MDA", bt.getSubscription().getProject().getName());
		Assertions.assertNotNull(bt.getId());

		// Calendar properties
		final Calendar calendar = bt.getCalendar();
		Assertions.assertEquals("France", calendar.getName());
		Assertions.assertNotNull(calendar.getId());
		Assertions.assertTrue(calendar.isAsDefault());

		// Non business hours ranges
		final List<BusinessHours> businessHours = bt.getBusinessHours();
		Assertions.assertTrue(isLazyInitialized(businessHours));
		Assertions.assertEquals(2, businessHours.size());

		// Non business hours range
		final BusinessHours businessRange1 = businessHours.get(0);
		Assertions.assertEquals(32400000, businessRange1.getStart());
		Assertions.assertEquals(43200000, businessRange1.getEnd());
		Assertions.assertNotNull(businessRange1.getId());

		final BusinessHours businessRange2 = businessHours.get(1);
		Assertions.assertEquals(46800000, businessRange2.getStart());
		Assertions.assertEquals(64800000, businessRange2.getEnd());
		Assertions.assertNotNull(businessRange2.getId());
	}
}

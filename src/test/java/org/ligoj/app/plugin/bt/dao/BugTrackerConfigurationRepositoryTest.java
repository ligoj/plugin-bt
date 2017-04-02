package org.ligoj.app.plugin.bt.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * {@link BugTrackerConfigurationRepository} test.
 */
@RunWith(SpringJUnit4ClassRunner.class)
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

	@Autowired
	private BusinessHoursRepository nonBusinessRangeRepository;

	@Before
	public void prepareData() throws IOException {
		persistEntities("csv", new Class[] { Calendar.class, Holiday.class, Node.class, Project.class,
				Subscription.class, BugTrackerConfiguration.class, BusinessHours.class, Sla.class },
				StandardCharsets.UTF_8.name());
	}

	@Test
	public void testOnlyForCoverageJpa() {
		Assert.assertNotNull(slaRepository.findAll().iterator().next().getConfiguration());
		Assert.assertNotNull(holidayRepository.findAll().iterator().next().getCalendar());
		Assert.assertNotNull(nonBusinessRangeRepository.findAll().iterator().next().getConfiguration());
		Assert.assertNotNull(repository.findAll().iterator().next().getSlas());
		Assert.assertNotNull(repository.findAll().iterator().next().getBusinessHours());
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
		Assert.assertEquals("MDA", bt.getSubscription().getProject().getName());
		Assert.assertNotNull(bt.getId());

		// Calendar properties
		final Calendar calendar = bt.getCalendar();
		Assert.assertEquals("France", calendar.getName());
		Assert.assertNotNull(calendar.getId());
		Assert.assertTrue(calendar.isAsDefault());

		// Non business hours ranges
		final List<BusinessHours> businessHours = bt.getBusinessHours();
		Assert.assertTrue(isLazyInitialized(businessHours));
		Assert.assertEquals(2, businessHours.size());

		// Non business hours range
		final BusinessHours businessRange1 = businessHours.get(0);
		Assert.assertEquals(32400000, businessRange1.getStart());
		Assert.assertEquals(43200000, businessRange1.getEnd());
		Assert.assertNotNull(businessRange1.getId());

		final BusinessHours businessRange2 = businessHours.get(1);
		Assert.assertEquals(46800000, businessRange2.getStart());
		Assert.assertEquals(64800000, businessRange2.getEnd());
		Assert.assertNotNull(businessRange2.getId());
	}
}

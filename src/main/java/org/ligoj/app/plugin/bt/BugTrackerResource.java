package org.ligoj.app.plugin.bt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.ligoj.app.api.ConfigurablePlugin;
import org.ligoj.app.plugin.bt.dao.BugTrackerConfigurationRepository;
import org.ligoj.app.plugin.bt.dao.BusinessHoursRepository;
import org.ligoj.app.plugin.bt.dao.CalendarRepository;
import org.ligoj.app.plugin.bt.dao.HolidayRepository;
import org.ligoj.app.plugin.bt.dao.SlaRepository;
import org.ligoj.app.plugin.bt.model.BugTrackerConfiguration;
import org.ligoj.app.plugin.bt.model.BusinessHours;
import org.ligoj.app.plugin.bt.model.Calendar;
import org.ligoj.app.plugin.bt.model.Holiday;
import org.ligoj.app.plugin.bt.model.Sla;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.app.resource.plugin.AbstractServicePlugin;
import org.ligoj.bootstrap.core.DescribedBean;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * The bug tracker service.
 */
@Path(BugTrackerResource.SERVICE_URL)
@Component
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class BugTrackerResource extends AbstractServicePlugin implements ConfigurablePlugin {

	/**
	 * Plug-in key.
	 */
	public static final String SERVICE_URL = BASE_URL + "/bt";

	/**
	 * Plug-in key.
	 */
	public static final String SERVICE_KEY = SERVICE_URL.replace('/', ':').substring(1);

	@Autowired
	private HolidayRepository holidayRepository;

	@Autowired
	private BugTrackerConfigurationRepository repository;

	@Autowired
	private SlaRepository slaRepository;

	@Autowired
	private SlaProcessor slaProcessor;

	@Autowired
	private CalendarRepository calendarRepository;

	@Autowired
	private BusinessHoursRepository businessRangeRepository;

	@Autowired
	protected ServicePluginLocator servicePluginLocator;

	@Autowired
	protected IdentifierHelper identifierHelper;

	@Override
	@Transactional(value = TxType.SUPPORTS)
	public String getKey() {
		return SERVICE_KEY;
	}

	@Override
	public void delete(final int subscription, final boolean deleteRemoteData) {
		repository.delete(getConfigurationBySubscription(subscription));
	}

	@Override
	public void create(final int subscription) {
		linkOrCreate(subscription);
	}

	@Override
	public void link(final int subscription) {
		linkOrCreate(subscription);
	}

	private void linkOrCreate(final int subscription) {
		// Add Configuration
		final BugTrackerConfiguration configuration = new BugTrackerConfiguration();
		configuration.setSubscription(subscriptionRepository.findOne(subscription));
		configuration.setCalendar(getDefaultCalendar());
		repository.saveAndFlush(configuration);

		// 9h to 18h
		final BusinessHours businessRange1 = new BusinessHours();
		businessRange1.setStart(8 * DateUtils.MILLIS_PER_HOUR);
		businessRange1.setEnd(18 * DateUtils.MILLIS_PER_HOUR);
		businessRange1.setConfiguration(configuration);
		businessRangeRepository.saveAndFlush(businessRange1);

		// Set a new SLA
		final Sla sla = new Sla();
		sla.setConfiguration(configuration);
		sla.setDescription("Closing : Open->Closed");
		sla.setStart("OPEN");
		sla.setStop("CLOSED");
		sla.setName("Closing");
		slaRepository.saveAndFlush(sla);
	}

	/**
	 * Return or create a new calendar.
	 */
	private Calendar getDefaultCalendar() {
		Calendar entity = calendarRepository.getDefault();
		if (entity == null) {
			entity = createDefaultCalendar();
		}
		return entity;
	}

	/**
	 * Persist and return a new calendar configuration.
	 */
	private Calendar createDefaultCalendar() {
		final Calendar entity = new Calendar();
		entity.setAsDefault(true);
		entity.setName("Default");
		calendarRepository.saveAndFlush(entity);

		final java.util.Calendar calendar = java.util.Calendar.getInstance();

		// January 1st and Christmas this year
		addHoliday(entity, calendar, 0, 1, "New year " + calendar.get(java.util.Calendar.YEAR));
		addHoliday(entity, calendar, 11, 25, "Christmas " + calendar.get(java.util.Calendar.YEAR));

		// January 1st and Christmas next year
		calendar.add(java.util.Calendar.YEAR, 1);
		addHoliday(entity, calendar, 0, 1, "New year " + calendar.get(java.util.Calendar.YEAR));
		addHoliday(entity, calendar, 11, 25, "Christmas " + calendar.get(java.util.Calendar.YEAR));
		return entity;
	}

	/**
	 * Add an holiday to the given {@link Calendar}
	 */
	private void addHoliday(final Calendar entity, final java.util.Calendar calendar, final int month,
			final int dayOfMonth, final String name) {
		calendar.set(java.util.Calendar.MONTH, month);
		calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
		final Holiday holiday = new Holiday();
		holiday.setDate(calendar.getTime());
		holiday.setCalendar(entity);
		holiday.setName(name);
		holidayRepository.saveAndFlush(holiday);
	}

	@Override
	public BtConfigurationVo getConfiguration(final int subscription) throws Exception {
		final BtConfigurationVo result = new BtConfigurationVo();
		final BugTrackerConfiguration configuration = getConfigurationBySubscription(subscription);
		result.setBusinessHours(new ArrayList<>(configuration.getBusinessHours()));
		final List<Sla> slas = configuration.getSlas();
		result.setSlas(slaProcessor.toSlaConfiguration(slas));
		result.setCalendar(configuration.getCalendar());

		// Provider data
		final BugTrackerServicePlugin provider = servicePluginLocator
				.getResourceExpected(configuration.getSubscription().getNode().getId(), BugTrackerServicePlugin.class);
		result.setTypes(new ArrayList<>(provider.getTypes(subscription)));
		result.setStatuses(identifierHelper.normalize(provider.getStatuses(subscription)));
		result.setPriorities(new ArrayList<>(provider.getPriorities(subscription)));
		result.setResolutions(new ArrayList<>(provider.getResolutions(subscription)));
		return result;
	}

	/**
	 * Add a new SLA to the given subscription.
	 * 
	 * @param vo
	 *            The SLA to attach.
	 * @return the associated parameter value as {@link String}
	 */
	@POST
	@Path("sla")
	@Consumes(MediaType.APPLICATION_JSON)
	public int addSla(final SlaEditionVo vo) {
		final Sla entity = new Sla();
		entity.setConfiguration(getConfigurationBySubscription(vo.getSubscription()));
		save(vo, entity);
		return entity.getId();
	}

	private void save(final SlaEditionVo vo, final Sla entity) {
		DescribedBean.copy(vo, entity);
		entity.setStop(StringUtils.join(identifierHelper.normalize(vo.getStop()), ','));
		entity.setStart(StringUtils.join(identifierHelper.normalize(vo.getStart()), ','));
		entity.setThreshold(vo.getThreshold());
		vo.setPause(ObjectUtils.defaultIfNull(vo.getPause(), new ArrayList<>()));
		vo.setPriorities(ObjectUtils.defaultIfNull(vo.getPriorities(), new ArrayList<>()));
		vo.setResolutions(ObjectUtils.defaultIfNull(vo.getResolutions(), new ArrayList<>()));
		vo.setTypes(ObjectUtils.defaultIfNull(vo.getTypes(), new ArrayList<>()));
		checkSlaBounds(vo);
		entity.setPause(StringUtils.join(identifierHelper.normalize(vo.getPause()), ','));
		entity.setPriorities(StringUtils.join(vo.getPriorities(), ','));
		entity.setResolutions(StringUtils.join(vo.getResolutions(), ','));
		entity.setTypes(StringUtils.join(vo.getTypes(), ','));
		slaRepository.saveAndFlush(entity);
	}

	/**
	 * Check SLA bounds
	 */
	private void checkSlaBounds(final SlaEditionVo vo) {
		if (!Collections.disjoint(vo.getPause(), vo.getStart())) {
			throw ValidationJsonException.newValidationJsonException("SlaBound", "start");
		}
		if (!Collections.disjoint(vo.getPause(), vo.getStop())) {
			throw ValidationJsonException.newValidationJsonException("SlaBound", "stop");
		}
	}

	/**
	 * Update the given SLA.
	 * 
	 * @param vo
	 *            The SLA to update.
	 */
	@PUT
	@Path("sla")
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateSla(final SlaEditionVo vo) {
		save(vo, slaRepository.findOneExpected(vo.getId()));
	}

	/**
	 * Delete the given SLA.
	 * 
	 * @param id
	 *            The SLA identifier.
	 */
	@DELETE
	@Path("sla/{id:\\d+}")
	public void deleteSla(@PathParam("id") final int id) {
		slaRepository.delete(id);
	}

	/**
	 * Update the business hours : only one range.
	 * 
	 * @param vo
	 *            The business hours to attach.
	 */
	@POST
	@Path("business-hours")
	@Consumes(MediaType.APPLICATION_JSON)
	public int addBusinessHours(final BusinessHoursEditionVo vo) {
		final BusinessHours entity = new BusinessHours();
		entity.setConfiguration(getConfigurationBySubscription(vo.getSubscription()));
		entity.setEnd(vo.getEnd());
		entity.setStart(vo.getStart());
		entity.setId(vo.getId());
		businessRangeRepository.saveAndFlush(entity);
		checkOverlaps(entity);
		return entity.getId();
	}

	/**
	 * Update the business hours : only one range.
	 * 
	 * @param vo
	 *            The business hours to update.
	 */
	@PUT
	@Path("business-hours")
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateBusinessHours(final BusinessHoursEditionVo vo) {
		addBusinessHours(vo);
	}

	/**
	 * Update the business hours : only one range.
	 * 
	 * @param id
	 *            The business hours identifier.
	 */
	@DELETE
	@Path("business-hours/{id:\\d+}")
	public void deleteBusinessHours(@PathParam("id") final int id) {
		final BusinessHours businessHours = businessRangeRepository.findOneExpected(id);

		// Check there is at least one business range
		if (businessHours.getConfiguration().getBusinessHours().size() == 1) {
			throw new BusinessException("service:bt:no-business-hours");
		}
		businessRangeRepository.delete(id);
	}

	/**
	 * Update the associated calendar.
	 * 
	 * @param subscription
	 *            The subscription identifier.
	 * @param calendar
	 *            The new calendar.
	 */
	@PUT
	@Path("calendar/{subscription:\\d+}/{calendar:\\d+}")
	public void setCalendar(@PathParam("subscription") final int subscription,
			@PathParam("calendar") final int calendar) {
		getConfigurationBySubscription(subscription).setCalendar(calendarRepository.findOneExpected(calendar));
	}

	/**
	 * Return available calendars
	 */
	@GET
	@Path("calendar")
	public List<Calendar> getAllCalendars() {
		return calendarRepository.findAll(new Sort("name"));
	}

	/**
	 * Check business hours overlaps.
	 */
	private void checkOverlaps(final BusinessHours newBusinessHours) {
		// Order business hours. BusinessHours is comparable
		final Set<BusinessHours> businessHours = new TreeSet<>(newBusinessHours.getConfiguration().getBusinessHours());
		businessHours.add(newBusinessHours);

		// Check the start<=end
		if (newBusinessHours.getEnd() <= newBusinessHours.getStart()) {
			throw ValidationJsonException.newValidationJsonException("Overlap", "stop");
		}

		// Check the overlaps
		BusinessHours previous = null;
		for (final BusinessHours step : businessHours) {
			if (previous != null && step.getStart() < previous.getEnd()) {
				throw ValidationJsonException.newValidationJsonException("Overlap", "start");
			}
			previous = step;
		}
	}

	/**
	 * Check and return the bug tracker configuration attached to the given
	 * subscription.
	 */
	private BugTrackerConfiguration getConfigurationBySubscription(final int subscription) {
		final BugTrackerConfiguration configuration = repository.findBySubscription(subscription);
		if (configuration == null) {
			throw new EntityNotFoundException(String.valueOf(subscription));
		}
		return configuration;
	}
}

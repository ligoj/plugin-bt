package org.ligoj.app.plugin.bt;

import java.util.List;

import org.ligoj.app.plugin.bt.model.BusinessHours;
import org.ligoj.app.plugin.bt.model.Calendar;

import lombok.Getter;
import lombok.Setter;

/**
 * Bug tracker configuration with textual data.
 */
@Getter
@Setter
public class BtConfigurationVo {

	/**
	 * Order by name SLAs
	 */
	private List<SlaConfiguration> slas;
	private Calendar calendar;
	
	/**
	 * Ordered by starting time, attached business hours.
	 */
	private List<BusinessHours> businessHours;

	/**
	 * Ordered by available statuses.
	 */
	private List<String> statuses;

	/**
	 * Ordered by available types.
	 */
	private List<String> types;

	/**
	 * Ordered by available priorities.
	 */
	private List<String> priorities;

	/**
	 * Ordered by available resolutions.
	 */
	private List<String> resolutions;
}

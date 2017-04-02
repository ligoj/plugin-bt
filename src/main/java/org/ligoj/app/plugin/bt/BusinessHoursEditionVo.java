package org.ligoj.app.plugin.bt;

import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.validator.constraints.Range;

import lombok.Getter;
import lombok.Setter;

/**
 * Business hours edition bean.
 */
@Getter
@Setter
public class BusinessHoursEditionVo {

	private Integer id;

	/**
	 * Business hours start, inclusive. Unix millisecond, 0 meaning start of day. 24*60*60*1000 meaning midnight.
	 */
	@Range(min = 0, max = DateUtils.MILLIS_PER_DAY)
	private long start;

	/**
	 * Business hours end, exclusive. Unix millisecond, 0 meaning start of day. 24*60*60*1000 meaning midnight.
	 */
	@Range(min = 0, max = DateUtils.MILLIS_PER_DAY)
	private long end;

	private int subscription;

}

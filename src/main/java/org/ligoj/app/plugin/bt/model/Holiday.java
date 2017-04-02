package org.ligoj.app.plugin.bt.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.ligoj.bootstrap.core.model.AbstractNamedEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * One holyday.
 */
@Getter
@Setter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "calendar", "date" }), name = "LIGOJ_BT_HOLIDAY")
public class Holiday extends AbstractNamedEntity<Integer> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 4795855466011388616L;

	/**
	 * The linked calendar
	 */
	@ManyToOne
	@JoinColumn(name = "calendar")
	@NotNull
	private Calendar calendar;

	/**
	 * Day off. Set to start of day : hours, minutes, seconds and milliseconds.
	 */
	@NotNull
	private Date date;

}

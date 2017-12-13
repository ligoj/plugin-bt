package org.ligoj.app.plugin.bt.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.ligoj.app.model.PluginConfiguration;
import org.ligoj.app.model.Subscription;
import org.ligoj.bootstrap.core.model.AbstractPersistable;

import lombok.Getter;
import lombok.Setter;

/**
 * A Bug Tracker subscription configuration.
 */
@Getter
@Setter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "subscription"), name = "LIGOJ_BT_CONFIGURATION")
public class BugTrackerConfiguration extends AbstractPersistable<Integer> implements PluginConfiguration {

	/**
	 * Attached {@link Subscription}.
	 */
	@OneToOne
	@NotNull
	@JoinColumn(name = "subscription")
	private Subscription subscription;

	/**
	 * Calendar configuration. Required for some computations.
	 */
	@ManyToOne
	private Calendar calendar;

	@OneToMany(mappedBy = "configuration", cascade = CascadeType.REMOVE)
	@OrderBy("name ASC")
	private List<Sla> slas;

	@OneToMany(mappedBy = "configuration", cascade = CascadeType.REMOVE)
	@OrderBy("start ASC")
	private List<BusinessHours> businessHours;

}

/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

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

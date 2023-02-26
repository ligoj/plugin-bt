/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.model;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.ligoj.app.model.Configurable;
import org.ligoj.bootstrap.core.model.AbstractDescribedEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * A SLA configuration.
 */
@Getter
@Setter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "configuration", "name" }), name = "LIGOJ_BT_SLA")
public class Sla extends AbstractDescribedEntity<Integer> implements Configurable<BugTrackerConfiguration, Integer> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Non business hour range. Required for some computations.
	 */
	@ManyToOne
	@NotNull
	@JoinColumn(name = "configuration")
	@JsonIgnore
	private BugTrackerConfiguration configuration;

	/**
	 * Status names to start this SLA, comma separated.
	 * 
	 * <code>SELECT DESCRIPTOR FROM `jiraworkflows` WHERE workflowname IN (SELECT WORKFLOW FROM `workflowschemeentity`
	 *      WHERE SCHEME = (SELECT SINK_NODE_ID FROM `nodeassociation` WHERE SOURCE_NODE_ENTITY = 'Project' AND
	 *      SOURCE_NODE_ID = '10000' AND SINK_NODE_ENTITY = 'WorkflowScheme')) | grep jira.status.id</code>
	 */
	@NotBlank
	@Length(max = 250)
	private String start;

	/**
	 * Status names to stop this SLA.
	 */
	@NotBlank
	@Length(max = 250)
	private String stop;

	/**
	 * Paused status names, comma separated.
	 */
	@Length(max = 250)
	private String pause;

	/**
	 * Starting status identifiers as {@link Set}.
	 */
	@Transient
	private Set<Integer> startAsSet;

	/**
	 * Ending status identifiers as {@link Set}.
	 */
	@Transient
	private Set<Integer> stopAsSet;

	/**
	 * Paused status identifiers as {@link Set}.
	 */
	@Transient
	private Set<Integer> pausedAsSet;

	/**
	 * Optional filtered types, comma separated. When empty, no filter.
	 */
	@Length(max = 250)
	private String types;

	/**
	 * Optional filtered types as {@link Set}. When empty, no filter.
	 */
	@Transient
	private Set<Integer> typesAsSet;

	/**
	 * Optional filtered priorities, comma separated. When empty, no filter.
	 */
	@Length(max = 250)
	private String priorities;

	/**
	 * Optional filtered priorities as {@link Set}. When empty, no filter.
	 */
	@Transient
	private Set<Integer> prioritiesAsSet;

	/**
	 * Optional filtered resolutions, comma separated. When empty, no filter.
	 */
	@Length(max = 250)
	private String resolutions;

	/**
	 * Optional filtered resolutions as {@link Set}. When empty, no filter.
	 */
	@Transient
	private Set<Integer> resolutionsAsSet;

	/**
	 * Threshold of this SLA : maximum millisecond to reach then {@link #stop}
	 * status. 0 means none.
	 */
	private long threshold;

}

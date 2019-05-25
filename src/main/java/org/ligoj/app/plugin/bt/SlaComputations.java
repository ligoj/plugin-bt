/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Computations of all SLA of a project.
 */
@Getter
@Setter
public class SlaComputations {

	/**
	 * SLA configurations.
	 */
	private List<SlaConfiguration> slaConfigurations;

	private List<IssueSla> issues;
}

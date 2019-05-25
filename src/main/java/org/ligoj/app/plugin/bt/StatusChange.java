/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt;

import org.ligoj.app.plugin.bt.model.ChangeItem;

import lombok.Getter;
import lombok.Setter;

/**
 * A status change date.
 */
@Getter
@Setter
public class StatusChange {

	/**
	 * New status identifier.
	 */
	private int status;

	/**
	 * Elapsed time within this status.
	 */
	private long elapsedtime;

	/**
	 * Snapshot total time cursor. Corresponds to the total working duration from the start of the computation context,
	 * and used to compute the position of this change. Take account the
	 * calendar and business hours and does not relate to the workflow of the related issue but to the first change of
	 * the first ticket.
	 */
	private long snapshotTime;

	/**
	 * Related change.
	 */
	private ChangeItem change;

}

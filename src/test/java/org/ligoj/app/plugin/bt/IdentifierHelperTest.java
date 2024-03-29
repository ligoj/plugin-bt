/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link IdentifierHelper} test class
 */
class IdentifierHelperTest {

	private final IdentifierHelper identifierHelper = new IdentifierHelper();

	@Test
	void testToIdentifiersMultipleWithNotFound() {
		final Map<Integer, String> allStatus = new HashMap<>();
		allStatus.put(1, "Open");
		allStatus.put(2, "Closed");
		allStatus.put(3, "OPEN");
		allStatus.put(4, "Resolved");
		allStatus.put(5, "IN PROGRESS");
		final Set<Integer> identifiers = identifierHelper.toIdentifiers("Resolved,In Progress2", allStatus);
		Assertions.assertEquals(1, identifiers.size());
		Assertions.assertTrue(identifiers.contains(4));
	}

	@Test
	void testToIdentifiersSimpleWithMultipleMatches() {
		final Map<Integer, String> allStatus = new HashMap<>();
		allStatus.put(1, "Open");
		allStatus.put(2, "Closed");
		allStatus.put(3, "OPEN");
		allStatus.put(4, "Resolved");
		allStatus.put(5, "IN PROGRESS");
		final Set<Integer> identifiers = identifierHelper.toIdentifiers("Open", allStatus);
		Assertions.assertEquals(2, identifiers.size());
		Assertions.assertTrue(identifiers.contains(1));
		Assertions.assertTrue(identifiers.contains(3));
	}

	@Test
	void testToIdentifiersSimple() {
		final Map<Integer, String> allStatus = new HashMap<>();
		allStatus.put(1, "Open");
		allStatus.put(2, "Closed");
		allStatus.put(3, "OPEN");
		allStatus.put(4, "Resolved");
		allStatus.put(5, "IN PROGRESS");
		final Set<Integer> identifiers = identifierHelper.toIdentifiers("Closed", allStatus);
		Assertions.assertEquals(1, identifiers.size());
		Assertions.assertTrue(identifiers.contains(2));
	}

}

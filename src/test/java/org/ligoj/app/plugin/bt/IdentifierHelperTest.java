package org.ligoj.app.plugin.bt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * {@link IdentifierHelper} test class
 */
public class IdentifierHelperTest {

	private final IdentifierHelper identifierHelper = new IdentifierHelper();

	@Test
	public void testToIdentifiersMultipleWithNotFound() {
		final Map<Integer, String> allStatus = new HashMap<>();
		allStatus.put(1, "Open");
		allStatus.put(2, "Closed");
		allStatus.put(3, "OPEN");
		allStatus.put(4, "Resolved");
		allStatus.put(5, "IN PROGRESS");
		final Set<Integer> identifiers = identifierHelper.toIdentifiers("Resolved,In Progress2", allStatus);
		Assert.assertEquals(1, identifiers.size());
		Assert.assertTrue(identifiers.contains(4));
	}

	@Test
	public void testToIdentifiersSimpleWihtMultipleMatches() {
		final Map<Integer, String> allStatus = new HashMap<>();
		allStatus.put(1, "Open");
		allStatus.put(2, "Closed");
		allStatus.put(3, "OPEN");
		allStatus.put(4, "Resolved");
		allStatus.put(5, "IN PROGRESS");
		final Set<Integer> identifiers = identifierHelper.toIdentifiers("Open", allStatus);
		Assert.assertEquals(2, identifiers.size());
		Assert.assertTrue(identifiers.contains(1));
		Assert.assertTrue(identifiers.contains(3));
	}

	@Test
	public void testToIdentifiersSimple() {
		final Map<Integer, String> allStatus = new HashMap<>();
		allStatus.put(1, "Open");
		allStatus.put(2, "Closed");
		allStatus.put(3, "OPEN");
		allStatus.put(4, "Resolved");
		allStatus.put(5, "IN PROGRESS");
		final Set<Integer> identifiers = identifierHelper.toIdentifiers("Closed", allStatus);
		Assert.assertEquals(1, identifiers.size());
		Assert.assertTrue(identifiers.contains(2));
	}

}

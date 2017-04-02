package org.ligoj.app.plugin.bt;

import java.text.Format;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.resource.NormalizeFormat;
import org.springframework.stereotype.Component;

/**
 * JIRA statues utilities
 */
@Component
public class IdentifierHelper {

	/**
	 * Split the comma separated string into list.
	 * 
	 * @param items
	 *            Items as string, comma separated. May be <code>null</code>.
	 * @return The split item.
	 */
	public List<String> asList(final String items) {
		return Arrays.asList(StringUtils.split(StringUtils.trimToEmpty(items), ','));
	}

	/**
	 * Normalize and sort given values.
	 * 
	 * @param values
	 *            The value to normalize.
	 * @return The normalized and sorted items.
	 */
	public List<String> normalize(final Collection<String> values) {
		return values.stream().map(new NormalizeFormat()::format).sorted().collect(Collectors.toList());
	}

	/**
	 * Transform the string containing comma separated texts to the
	 * corresponding identifiers.
	 *
	 * @param texts
	 *            the string containing comma separated texts.
	 * @param mapping
	 *            The mapping associating identifier and text.
	 * @return the corresponding identifiers. Order is preserved.
	 */
	public final Set<Integer> toIdentifiers(final String texts, final Map<Integer, String> mapping) {
		final Set<Integer> result = new LinkedHashSet<>();
		for (final String text : StringUtils.split(StringUtils.trimToEmpty(texts), ',')) {
			extracted(mapping, text, result);
		}
		return result;
	}

	private void extracted(final Map<Integer, String> allStatus, final String originalStatus,
			final Set<Integer> targetIndentifiers) {
		final Format format = new NormalizeFormat();
		final String text = format.format(originalStatus);
		for (final Entry<Integer, String> status : allStatus.entrySet()) {
			if (format.format(status.getValue()).equals(text)) {
				targetIndentifiers.add(status.getKey());
			}
		}
	}

}

package skinxyou_backend.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/** Builds readable latin URL slugs, transliterating Russian and Kazakh Cyrillic. */
public final class Slugs {

	private static final int MAX_LENGTH = 200;

	private static final Map<Character, String> CYRILLIC = Map.ofEntries(
			Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"),
			Map.entry('д', "d"), Map.entry('е', "e"), Map.entry('ё', "e"), Map.entry('ж', "zh"),
			Map.entry('з', "z"), Map.entry('и', "i"), Map.entry('й', "y"), Map.entry('к', "k"),
			Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"), Map.entry('о', "o"),
			Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"), Map.entry('т', "t"),
			Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "kh"), Map.entry('ц', "ts"),
			Map.entry('ч', "ch"), Map.entry('ш', "sh"), Map.entry('щ', "shch"), Map.entry('ъ', ""),
			Map.entry('ы', "y"), Map.entry('ь', ""), Map.entry('э', "e"), Map.entry('ю', "yu"),
			Map.entry('я', "ya"),
			// Kazakh
			Map.entry('ә', "a"), Map.entry('ғ', "g"), Map.entry('қ', "q"), Map.entry('ң', "n"),
			Map.entry('ө', "o"), Map.entry('ұ', "u"), Map.entry('ү', "u"), Map.entry('һ', "h"),
			Map.entry('і', "i"));

	private Slugs() {
	}

	/** Returns a slug, or {@code "item"} when the input has no usable characters. */
	public static String slugify(String input) {
		if (input == null) {
			return "item";
		}
		StringBuilder out = new StringBuilder();
		for (char c : input.toLowerCase(Locale.ROOT).toCharArray()) {
			String mapped = CYRILLIC.get(c);
			out.append(mapped != null ? mapped : String.valueOf(c));
		}
		String slug = Normalizer.normalize(out, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("(^-+|-+$)", "");
		if (slug.length() > MAX_LENGTH) {
			slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
		}
		return slug.isEmpty() ? "item" : slug;
	}
}

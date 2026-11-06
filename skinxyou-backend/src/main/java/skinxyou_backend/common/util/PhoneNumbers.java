package skinxyou_backend.common.util;

import java.util.Optional;

/** Kazakhstan mobile numbers: country code 7, ten national digits starting with 7. */
public final class PhoneNumbers {

	private PhoneNumbers() {
	}

	/**
	 * Accepts "+7 776 005 06 25", "87760050625", "7760050625" and similar; returns "+77760050625",
	 * or empty when the input is not a Kazakhstan mobile number.
	 */
	public static Optional<String> normalizeKz(String input) {
		if (input == null) {
			return Optional.empty();
		}
		String digits = input.replaceAll("\\D", "");
		if (digits.length() == 11 && (digits.startsWith("7") || digits.startsWith("8"))) {
			digits = digits.substring(1);
		}
		if (digits.length() != 10 || digits.charAt(0) != '7') {
			return Optional.empty();
		}
		return Optional.of("+7" + digits);
	}
}

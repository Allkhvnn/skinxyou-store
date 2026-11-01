package skinxyou_backend.common.util;

import java.util.Optional;

/** Kazakhstan mobile numbers: country code 7, ten national digits starting with 7. */
public final class PhoneNumbers {

	private PhoneNumbers() {
	}

	/**
	 * Accepts "+7 706 605 97 13", "87066059713", "7066059713" and similar; returns "+77066059713",
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

package skinxyou_backend.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumbersTest {

	@ParameterizedTest
	@ValueSource(strings = {"+7 776 005 06 25", "87760050625", "77760050625", "7760050625", "+7 (776) 005-06-25"})
	void normalizesKazakhstanMobileNumbers(String input) {
		assertThat(PhoneNumbers.normalizeKz(input)).contains("+77760050625");
	}

	@ParameterizedTest
	@CsvSource({"12345", "+7 495 123 45 67", "+1 202 555 0100", "''"})
	void rejectsOtherNumbers(String input) {
		assertThat(PhoneNumbers.normalizeKz(input)).isEmpty();
	}
}

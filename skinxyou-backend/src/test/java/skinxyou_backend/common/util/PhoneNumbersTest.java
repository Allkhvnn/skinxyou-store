package skinxyou_backend.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumbersTest {

	@ParameterizedTest
	@ValueSource(strings = {"+7 706 605 97 13", "87066059713", "77066059713", "7066059713", "+7 (706) 605-97-13"})
	void normalizesKazakhstanMobileNumbers(String input) {
		assertThat(PhoneNumbers.normalizeKz(input)).contains("+77066059713");
	}

	@ParameterizedTest
	@CsvSource({"12345", "+7 495 123 45 67", "+1 202 555 0100", "''"})
	void rejectsOtherNumbers(String input) {
		assertThat(PhoneNumbers.normalizeKz(input)).isEmpty();
	}
}

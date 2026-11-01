package skinxyou_backend.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SlugsTest {

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"Apothe moist Nourshing Treatment | apothe-moist-nourshing-treatment",
			"Крем для лица 50 мл              | krem-dlya-litsa-50-ml",
			"Қазақ өнімі                      | qazaq-onimi",
			"COSRX  Snail 96%  Essence!       | cosrx-snail-96-essence",
			"Crème Brûlée                     | creme-brulee",
			"---                              | item",
	})
	void slugify(String input, String expected) {
		assertThat(Slugs.slugify(input)).isEqualTo(expected);
	}
}

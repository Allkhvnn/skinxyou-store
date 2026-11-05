package skinxyou_backend.admin;

import java.util.Optional;

/** Detects the image type from the file's own bytes; the client's declared content type is not trusted. */
enum ImageFormat {
	JPEG("image/jpeg", "jpg"),
	PNG("image/png", "png"),
	WEBP("image/webp", "webp");

	final String contentType;
	final String extension;

	ImageFormat(String contentType, String extension) {
		this.contentType = contentType;
		this.extension = extension;
	}

	static Optional<ImageFormat> detect(byte[] b) {
		if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
			return Optional.of(JPEG);
		}
		if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
			return Optional.of(PNG);
		}
		if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
				&& b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
			return Optional.of(WEBP);
		}
		return Optional.empty();
	}
}

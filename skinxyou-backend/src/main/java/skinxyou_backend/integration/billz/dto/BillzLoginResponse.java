package skinxyou_backend.integration.billz.dto;

public record BillzLoginResponse(Data data) {

	public record Data(String accessToken, String refreshToken, String tokenType, Long expiresIn) {
	}
}

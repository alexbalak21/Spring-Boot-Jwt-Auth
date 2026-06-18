package app.dto;

public final class Auth {

    private Auth() {}

    public record LoginRequest(String username, String password) {}

    public record RegisterRequest(String username, String password) {}

    public record RefreshRequest(String refreshToken) {}

    public record TokenResponse(String accessToken, String refreshToken, String tokenType) {
        public TokenResponse(String accessToken, String refreshToken) {
            this(accessToken, refreshToken, "Bearer");
        }
    }

    public record MessageResponse(String message) {}
}

package ar.com.anepanet.planificator.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "planificator.jwt")
public class JwtProperties {

    private String secret = "planificator-dev-secret-change-me-32chars";
    private long expirationHours = 12;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationHours() {
        return expirationHours;
    }

    public void setExpirationHours(long expirationHours) {
        this.expirationHours = expirationHours;
    }
}

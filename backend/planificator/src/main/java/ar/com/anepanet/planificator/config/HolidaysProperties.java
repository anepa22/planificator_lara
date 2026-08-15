package ar.com.anepanet.planificator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "planificator.holidays")
public class HolidaysProperties {

    private String baseUrl = "https://api.argentinadatos.com/v1/feriados";
    private long cacheTtlHours = 24;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getCacheTtlHours() {
        return cacheTtlHours;
    }

    public void setCacheTtlHours(long cacheTtlHours) {
        this.cacheTtlHours = cacheTtlHours;
    }
}

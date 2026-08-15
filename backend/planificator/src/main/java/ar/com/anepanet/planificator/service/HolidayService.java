package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.config.HolidaysProperties;
import ar.com.anepanet.planificator.domain.Holiday;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@EnableConfigurationProperties(HolidaysProperties.class)
public class HolidayService {

    private static final Logger log = LoggerFactory.getLogger(HolidayService.class);

    private final HolidaysProperties props;
    private final RestClient restClient;
    private final Map<Integer, CacheEntry> cache = new ConcurrentHashMap<>();

    public HolidayService(HolidaysProperties props) {
        this.props = props;
        this.restClient = RestClient.create();
    }

    public List<Holiday> forYear(int year) {
        CacheEntry cached = cache.get(year);
        if (cached != null && !cached.isExpired(props.getCacheTtlHours())) {
            return cached.holidays();
        }

        try {
            List<Holiday> fetched = fetchYear(year);
            cache.put(year, new CacheEntry(fetched, Instant.now()));
            return fetched;
        } catch (Exception e) {
            log.warn("No se pudieron obtener feriados de {} para {}: {}",
                    props.getBaseUrl(), year, e.getMessage());
            if (cached != null) {
                return cached.holidays();
            }
            return List.of();
        }
    }

    private List<Holiday> fetchYear(int year) {
        String url = trimTrailingSlash(props.getBaseUrl()) + "/" + year;
        ArgentinaDatosHoliday[] body = restClient.get()
                .uri(url)
                .retrieve()
                .body(ArgentinaDatosHoliday[].class);
        if (body == null || body.length == 0) {
            return List.of();
        }
        return Arrays.stream(body)
                .filter(h -> h.fecha() != null && h.nombre() != null && !h.nombre().isBlank())
                .map(h -> new Holiday(h.fecha(), h.nombre().trim(), h.tipo()))
                .toList();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record ArgentinaDatosHoliday(LocalDate fecha, String tipo, String nombre) {}

    private record CacheEntry(List<Holiday> holidays, Instant loadedAt) {
        boolean isExpired(long ttlHours) {
            return Instant.now().isAfter(loadedAt.plusSeconds(Math.max(1, ttlHours) * 3600L));
        }
    }
}

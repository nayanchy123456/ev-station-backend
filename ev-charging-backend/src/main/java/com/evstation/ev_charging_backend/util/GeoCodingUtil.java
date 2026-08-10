package com.evstation.ev_charging_backend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class GeoCodingUtil {

    @Value("${LOCATIONIQ_API_KEY}")
    private String apiKey;

    /**
     * Geocodes an address, with a fallback: if the full address doesn't
     * resolve, progressively strips the first comma-separated segment and
     * retries (e.g. "New Road, Kathmandu, Nepal" -> "Kathmandu, Nepal" ->
     * "Nepal"). This handles cases like street-level addresses that aren't
     * in OSM's data, falling back to a broader match rather than failing
     * entirely.
     */
    public double[] getLatLngFromAddress(String address) {
        String currentAddress = address;

        while (currentAddress != null && !currentAddress.isBlank()) {
            double[] result = tryGeocode(currentAddress);
            if (result[0] != 0.0 || result[1] != 0.0) {
                return result;
            }
            currentAddress = stripFirstSegment(currentAddress);
        }

        return new double[]{0.0, 0.0};
    }

    private double[] tryGeocode(String address) {
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
           String url = "https://us1.locationiq.com/v1/search?key=" + apiKey
                    + "&q=" + encoded + "&format=json&countrycodes=np";

            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            if (root.isArray() && root.size() > 0) {
                double lat = root.get(0).get("lat").asDouble();
                double lon = root.get(0).get("lon").asDouble();
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new double[]{0.0, 0.0};
    }

    /**
     * "New Road, Kathmandu, Nepal" -> "Kathmandu, Nepal"
     * "Kathmandu, Nepal" -> "Nepal"
     * "Nepal" -> null (nothing left to strip)
     */
    private String stripFirstSegment(String address) {
        int commaIndex = address.indexOf(',');
        if (commaIndex == -1) {
            return null; // no more segments to strip
        }
        return address.substring(commaIndex + 1).trim();
    }
}
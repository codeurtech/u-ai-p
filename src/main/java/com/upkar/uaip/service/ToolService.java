package com.upkar.uaip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class ToolService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unused")
    @Tool(name = "get_current_date_time", description = "Get the current date and time")
    public String currentDateTimeTool(@ToolParam(description = "The city or region (e.g. 'Oxford, United Kingdom')") String location) {
        if (location == null || location.isBlank()) {
            return "Location parameter is required (e.g. 'Oxford, United Kingdom').";
        }

        String apiKey = System.getenv("TIMEZONE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return "Timezone API key not configured. Set the TIMEZONE_API_KEY environment variable.";
        }

        try {
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String url = "https://timezone.abstractapi.com/v1/current_time/?api_key=" + encodedKey + "&location=" + encodedLocation;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                JsonNode root = objectMapper.readTree(response.body());

                String requestedLocation = root.path("requested_location").asText(location);
                String dateTime = root.path("datetime").asText(null);
                String tzName = root.path("timezone_name").asText("");
                String tzAbbrev = root.path("timezone_abbreviation").asText("");
                String gmtOffset = root.path("gmt_offset").asText("");

                if (dateTime != null) {
                    StringBuilder meta = new StringBuilder();
                    if (!tzAbbrev.isEmpty()) {
                        meta.append(tzAbbrev);
                    }
                    if (!tzName.isEmpty()) {
                        if (!meta.isEmpty()) meta.append(" - ");
                        meta.append(tzName);
                    }
                    if (!gmtOffset.isEmpty()) {
                        if (!meta.isEmpty()) meta.append(", ");
                        meta.append("GMT" + gmtOffset);
                    }

                    String metaStr = !meta.isEmpty() ? " (" + meta.toString() + ")" : "";
                    return String.format("Current time for '%s'%s: %s", requestedLocation, metaStr, dateTime);
                }

                return String.format("Current time for '%s' (raw): %s", location, response.body());
            } else {
                return "Timezone API request failed: HTTP " + status + " - " + response.body();
            }
        } catch (Exception e) {
            return "Error calling Timezone API: " + e.getMessage();
        }
    }

    @SuppressWarnings("unused")
    @Tool(name = "get_weather", description = "Get current weather for a given city")
    public String weatherTool(@ToolParam(description = "The city or location for which weather needs to be looked up") String location) {
        String apiKey = System.getenv("WEATHER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return "Weather API key not configured. Set the WEATHER_API_KEY environment variable.";
        }
        if (location == null || location.isBlank()) {
            return "City parameter is required.";
        }

        try {
            String q = URLEncoder.encode(location, StandardCharsets.UTF_8);
            String key = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String url = "https://api.weatherapi.com/v1/current.json?q=" + q + "&key=" + key;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return response.body();
            } else {
                return "Weather API request failed: HTTP " + status + " - " + response.body();
            }
        } catch (Exception e) {
            return "Error calling Weather API: " + e.getMessage();
        }
    }

}

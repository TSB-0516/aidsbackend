package com.example.aids.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;

@Service
public class AffiliateService {

    @Value("${affiliate.api.url}")
    private String apiUrl;

    @Value("${affiliate.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private long lastAffiliateUpdate = 0;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        initializeAffiliateData();
        scheduleDataFetch();
    }

    public Map<String, Object> fetchAffiliateStats() {
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("apikey", apiKey);

            // Create HTTP entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Make API call
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            Map<String, Object> responseData = response.getBody();
            
            if (responseData != null && !Boolean.TRUE.equals(responseData.get("error"))) {
                saveDataToJson(responseData);
                return responseData;
            } else {
                System.err.println("❌ API returned error: " + responseData);
                return createErrorResponse("API returned error response");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to fetch affiliate stats: " + e.getMessage());
            return createErrorResponse("Failed to fetch affiliate stats");
        }
    }

    public Map<String, Object> getLeaderboardData() {
        try {
            File leaderboardFile = new File("leaderboard_data.json");
            if (leaderboardFile.exists()) {
                Map<String, Object> leaderboardData = objectMapper.readValue(leaderboardFile, Map.class);
                
                // Calculate next update time (7 days from affiliate_data.json last modified)
                File affiliateFile = new File("affiliate_data.json");
                long nextUpdate = affiliateFile.lastModified() + TimeUnit.DAYS.toMillis(7);
                leaderboardData.put("nextAffiliateUpdate", nextUpdate);
                
                // If the data is older than 6 minutes, fetch new data
                long lastUpdated = Long.valueOf(leaderboardData.get("lastUpdated").toString());
                if (System.currentTimeMillis() - lastUpdated > TimeUnit.MINUTES.toMillis(6)) {
                    fetchAffiliateStats();
                    leaderboardData = objectMapper.readValue(leaderboardFile, Map.class);
                }
                return leaderboardData;
            }
            // If file doesn't exist, fetch new data
            fetchAffiliateStats();
            return objectMapper.readValue(leaderboardFile, Map.class);
        } catch (IOException e) {
            System.err.println("❌ Failed to read leaderboard data: " + e.getMessage());
            return createErrorResponse("Failed to read leaderboard data");
        }
    }

    private void saveDataToJson(Map<String, Object> data) {
        try {
            // Save current data
            objectMapper.writeValue(new File("current_data.json"), data);

            // Process and save leaderboard data
            if (data.containsKey("data")) {
                Map<String, Object> innerData = (Map<String, Object>) data.get("data");
                if (innerData.containsKey("summarizedBets")) {
                    List<Map<String, Object>> currentBets = (List<Map<String, Object>>) innerData.get("summarizedBets");
                    
                    // Read previous data from affiliate_data.json
                    Map<String, Object> previousData = new HashMap<>();
                    File affiliateFile = new File("affiliate_data.json");
                    if (affiliateFile.exists()) {
                        previousData = objectMapper.readValue(affiliateFile, Map.class);
                    }

                    // Check if it's time to update affiliate_data.json (7 days passed)
                    long currentTime = System.currentTimeMillis();
                    if (!affiliateFile.exists() || currentTime - affiliateFile.lastModified() > TimeUnit.DAYS.toMillis(7)) {
                        // Store current leaderboard as previous leaderboard
                        File leaderboardFile = new File("leaderboard_data.json");
                        if (leaderboardFile.exists()) {
                            Map<String, Object> currentLeaderboard = objectMapper.readValue(leaderboardFile, Map.class);
                            savePreviousLeaderboard(currentLeaderboard);
                        }

                        // Store new baseline data
                        objectMapper.writeValue(affiliateFile, data);
                        lastAffiliateUpdate = currentTime;
                        System.out.println("📝 Updated affiliate_data.json with new baseline data");
                        
                        // Reset leaderboard for new week
                        List<Map<String, Object>> leaderboardEntries = new ArrayList<>();
                        Map<String, Object> leaderboardData = new HashMap<>();
                        leaderboardData.put("lastUpdated", currentTime);
                        leaderboardData.put("nextAffiliateUpdate", currentTime + TimeUnit.DAYS.toMillis(7));
                        leaderboardData.put("leaderboard", leaderboardEntries);
                        objectMapper.writeValue(new File("leaderboard_data.json"), leaderboardData);
                        System.out.println("🔄 Reset leaderboard for new week");
                        return;
                    }

                    // Get previous bets
                    List<Map<String, Object>> previousBets = new ArrayList<>();
                    if (previousData.containsKey("data")) {
                        Map<String, Object> prevInnerData = (Map<String, Object>) previousData.get("data");
                        if (prevInnerData.containsKey("summarizedBets")) {
                            previousBets = (List<Map<String, Object>>) prevInnerData.get("summarizedBets");
                        }
                    }

                    // Calculate differences
                    List<Map<String, Object>> leaderboardEntries = new ArrayList<>();
                    for (Map<String, Object> currentBet : currentBets) {
                        Map<String, Object> user = (Map<String, Object>) currentBet.get("user");
                        Long userId = Long.valueOf(user.get("id").toString());
                        Long currentWager = Long.valueOf(currentBet.get("wager").toString());
                        Long currentBetsCount = Long.valueOf(currentBet.get("bets").toString());

                        // Find previous stats for this user
                        Map<String, Object> previousBet = previousBets.stream()
                            .filter(bet -> Long.valueOf(((Map<String, Object>)bet.get("user")).get("id").toString()).equals(userId))
                            .findFirst()
                            .orElse(null);

                        // Calculate differences
                        Long previousWager = previousBet != null ? Long.valueOf(previousBet.get("wager").toString()) : 0L;
                        Long previousBetsCount = previousBet != null ? Long.valueOf(previousBet.get("bets").toString()) : 0L;
                        Long wagerDiff = currentWager - previousWager;
                        Long betsDiff = currentBetsCount - previousBetsCount;

                        // Only add to leaderboard if there's progress
                        if (wagerDiff > 0) {
                            Map<String, Object> leaderboardEntry = new HashMap<>();
                            leaderboardEntry.put("user", user);
                            leaderboardEntry.put("wager", wagerDiff);
                            leaderboardEntry.put("bets", betsDiff);
                            leaderboardEntries.add(leaderboardEntry);
                        }
                    }

                    // Sort by wager difference in descending order
                    leaderboardEntries.sort((a, b) -> {
                        Long wagerA = Long.valueOf(a.get("wager").toString());
                        Long wagerB = Long.valueOf(b.get("wager").toString());
                        return wagerB.compareTo(wagerA);
                    });

                    // Create leaderboard data structure
                    Map<String, Object> leaderboardData = new HashMap<>();
                    leaderboardData.put("lastUpdated", System.currentTimeMillis());
                    leaderboardData.put("leaderboard", leaderboardEntries);
                    
                    // Add next affiliate update timestamp
                    long nextAffiliateUpdate = lastAffiliateUpdate + TimeUnit.DAYS.toMillis(7);
                    leaderboardData.put("nextAffiliateUpdate", nextAffiliateUpdate);

                    // Save leaderboard data
                    objectMapper.writeValue(new File("leaderboard_data.json"), leaderboardData);
                }
            }

            // Update affiliate data every 7 days
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAffiliateUpdate > TimeUnit.DAYS.toMillis(7)) {
                objectMapper.writeValue(new File("affiliate_data.json"), data);
                lastAffiliateUpdate = currentTime;
            }

        } catch (IOException e) {
            System.err.println("❌ Failed to save data to JSON: " + e.getMessage());
        }
    }

    private void savePreviousLeaderboard(Map<String, Object> currentLeaderboard) {
        try {
            // Add timestamp when this leaderboard was archived
            currentLeaderboard.put("archivedAt", System.currentTimeMillis());
            
            // Read existing previous leaderboards
            List<Map<String, Object>> previousLeaderboards = new ArrayList<>();
            File previousFile = new File("previous_leaderboard.json");
            if (previousFile.exists()) {
                previousLeaderboards = objectMapper.readValue(previousFile, List.class);
            }
            
            // Add current leaderboard to the start of the list
            previousLeaderboards.add(0, currentLeaderboard);
            
            // Keep only the last 4 weeks of leaderboards
            if (previousLeaderboards.size() > 4) {
                previousLeaderboards = previousLeaderboards.subList(0, 4);
            }
            
            // Save to file
            objectMapper.writeValue(previousFile, previousLeaderboards);
            System.out.println("📚 Archived previous leaderboard");
        } catch (IOException e) {
            System.err.println("❌ Failed to save previous leaderboard: " + e.getMessage());
        }
    }

    // New endpoint to get previous leaderboards
    public List<Map<String, Object>> getPreviousLeaderboards() {
        try {
            File previousFile = new File("previous_leaderboard.json");
            if (previousFile.exists()) {
                return objectMapper.readValue(previousFile, List.class);
            }
            return new ArrayList<>();
        } catch (IOException e) {
            System.err.println("❌ Failed to read previous leaderboards: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void initializeAffiliateData() {
        File affiliateFile = new File("affiliate_data.json");
        if (!affiliateFile.exists()) {
            System.out.println("⚡ First time run: Creating affiliate_data.json");
            fetchAffiliateStats(); // Fetch and store initial data
        } else {
            lastAffiliateUpdate = affiliateFile.lastModified();
            System.out.println("⏳ Affiliate data last updated on: " + LocalDate.ofEpochDay(lastAffiliateUpdate / (1000 * 60 * 60 * 24)));
        }
    }

    private void scheduleDataFetch() {
        System.out.println("⏰ Scheduling automatic data fetch every 5 minutes 10 seconds...");
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("\n🕒 Running scheduled API fetch at " + LocalDate.now());
                fetchAffiliateStats();
            }
        }, 0, (5 * 60 + 10) * 1000); // Run every 5 minutes 10 seconds
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("msg", message);
        return response;
    }
}

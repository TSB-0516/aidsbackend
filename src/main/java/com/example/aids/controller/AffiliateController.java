package com.example.aids.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.example.aids.service.AffiliateService;

@RestController
@RequestMapping("/affiliate")
@CrossOrigin(origins = "*")
public class AffiliateController {

    private final AffiliateService affiliateService;

    public AffiliateController(AffiliateService affiliateService) {
        this.affiliateService = affiliateService;
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<Map<String, Object>> getLeaderboard() {
        return ResponseEntity.ok(affiliateService.getLeaderboardData());
    }
    
    @PostMapping("/fetch")
    public Map<String, Object> fetchAffiliateStats() {
        return affiliateService.fetchAffiliateStats();
    }

    @GetMapping("/previous-leaderboards")
    public ResponseEntity<List<Map<String, Object>>> getPreviousLeaderboards() {
        return ResponseEntity.ok(affiliateService.getPreviousLeaderboards());
    }
}

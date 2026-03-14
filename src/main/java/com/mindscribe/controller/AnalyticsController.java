package com.mindscribe.controller;

import com.mindscribe.model.h2.JournalEntry;
import com.mindscribe.repository.h2.JournalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private JournalRepository journalRepository;

    @GetMapping("/mood-distribution")
    public ResponseEntity<?> getMoodDistribution(@RequestParam(required = false) String username) {
        try {
            List<JournalEntry> allEntries;
            if (username != null && !username.trim().isEmpty()) {
                allEntries = journalRepository.findByUsernameOrderByCreatedAtDesc(username);
            } else {
                allEntries = journalRepository.findAllByOrderByCreatedAtDesc();
            }
            
            Map<String, Long> moodCounts = new HashMap<>();
            moodCounts.put("positive", 0L);
            moodCounts.put("negative", 0L);
            moodCounts.put("neutral", 0L);
            
            for (JournalEntry entry : allEntries) {
                String sentiment = entry.getSentiment();
                if (sentiment != null) {
                    moodCounts.put(sentiment, moodCounts.getOrDefault(sentiment, 0L) + 1);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("moodDistribution", moodCounts);
            response.put("totalEntries", allEntries.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to load mood distribution");
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/sentiment-trends")
    public ResponseEntity<?> getSentimentTrends(@RequestParam(required = false) String username) {
        try {
            List<JournalEntry> allEntries;
            if (username != null && !username.trim().isEmpty()) {
                allEntries = journalRepository.findByUsernameOrderByCreatedAtDesc(username);
            } else {
                allEntries = journalRepository.findAllByOrderByCreatedAtDesc();
            }
            
            Map<String, Map<String, Long>> trends = new HashMap<>();
            
            // Group by date and sentiment
            for (JournalEntry entry : allEntries) {
                if (entry.getSentiment() != null) {
                    LocalDate date = entry.getCreatedAt().toLocalDate();
                    String dateKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    
                    trends.computeIfAbsent(dateKey, k -> new HashMap<>())
                           .put(entry.getSentiment(), trends.get(dateKey).getOrDefault(entry.getSentiment(), 0L) + 1);
                }
            }
            
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to load sentiment trends");
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(@RequestParam(required = false) String username) {
        try {
            List<JournalEntry> allEntries;
            if (username != null && !username.trim().isEmpty()) {
                allEntries = journalRepository.findByUsernameOrderByCreatedAtDesc(username);
            } else {
                allEntries = journalRepository.findAllByOrderByCreatedAtDesc();
            }
            
            Map<String, Object> stats = new HashMap<>();
            
            // Total entries
            stats.put("totalEntries", allEntries.size());
            
            // Average sentiment score
            double avgSentimentScore = allEntries.stream()
                .filter(entry -> entry.getSentimentScore() != null)
                .mapToDouble(JournalEntry::getSentimentScore)
                .average()
                .orElse(0.5);
            stats.put("averageSentimentScore", avgSentimentScore);
            
            // Most common sentiment
            Map<String, Long> sentimentCounts = allEntries.stream()
                .filter(entry -> entry.getSentiment() != null)
                .collect(Collectors.groupingBy(JournalEntry::getSentiment, Collectors.counting()));
            
            String mostCommonSentiment = sentimentCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("neutral");
            stats.put("mostCommonSentiment", mostCommonSentiment);
            
            // Entries this week
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            long entriesThisWeek = allEntries.stream()
                .filter(entry -> entry.getCreatedAt().isAfter(weekAgo))
                .count();
            stats.put("entriesThisWeek", entriesThisWeek);
            
            // Entries this month
            LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
            long entriesThisMonth = allEntries.stream()
                .filter(entry -> entry.getCreatedAt().isAfter(monthAgo))
                .count();
            stats.put("entriesThisMonth", entriesThisMonth);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to load statistics");
            return ResponseEntity.status(500).body(error);
        }
    }
}

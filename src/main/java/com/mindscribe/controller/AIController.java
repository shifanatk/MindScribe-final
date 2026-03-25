package com.mindscribe.controller;

import com.mindscribe.model.h2.JournalEntry;
import com.mindscribe.repository.h2.JournalRepository;
import com.mindscribe.service.SentimentAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    @Autowired
    private SentimentAnalysisService sentimentAnalysisService;
    
    @Autowired
    private JournalRepository journalRepository;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeText(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Content is required");
                return ResponseEntity.badRequest().body(error);
            }

            String sentiment = sentimentAnalysisService.analyzeSentiment(content);
            double score = sentimentAnalysisService.getSentimentScore(content);
            
            Map<String, Object> result = new HashMap<>();
            result.put("sentiment", sentiment);
            result.put("score", score);
            result.put("insight", generateInsight(sentiment, score));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Analysis failed: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/patterns")
    public ResponseEntity<?> getPatternAnalysis(@RequestParam(required = false) String username) {
        try {
            List<JournalEntry> entries;
            if (username != null && !username.trim().isEmpty()) {
                entries = journalRepository.findByUsernameOrderByCreatedAtDesc(username);
            } else {
                entries = journalRepository.findAllByOrderByCreatedAtDesc();
            }
            
            if (entries.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("pattern", "No journal entries found");
                result.put("dominantEmotion", "No Data");
                result.put("emotionalStability", "N/A");
                result.put("wellnessScore", "N/A");
                return ResponseEntity.ok(result);
            }
            
            // Analyze real patterns from entries
            Map<String, Object> result = analyzeRealPatterns(entries);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to analyze patterns: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    private Map<String, Object> analyzeRealPatterns(List<JournalEntry> entries) {
        Map<String, Object> result = new HashMap<>();
        
        // Get last 7 days for pattern analysis
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<JournalEntry> recentEntries = entries.stream()
            .filter(e -> e.getCreatedAt().isAfter(weekAgo))
            .collect(Collectors.toList());
        
        if (recentEntries.isEmpty()) {
            result.put("pattern", "No recent entries for pattern analysis");
            result.put("dominantEmotion", "No Data");
            result.put("emotionalStability", "N/A");
            result.put("wellnessScore", "N/A");
            return result;
        }
        
        // Analyze sentiment distribution
        Map<String, Long> sentimentCounts = recentEntries.stream()
            .filter(e -> e.getSentiment() != null)
            .collect(Collectors.groupingBy(JournalEntry::getSentiment, Collectors.counting()));
        
        // Find dominant emotion
        String dominantEmotion = sentimentCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("neutral");
        
        // Calculate emotional stability (consistency of sentiment)
        double avgSentimentScore = recentEntries.stream()
            .filter(e -> e.getSentimentScore() != null)
            .mapToDouble(JournalEntry::getSentimentScore)
            .average()
            .orElse(0.5);
        
        double variance = recentEntries.stream()
            .filter(e -> e.getSentimentScore() != null)
            .mapToDouble(e -> Math.pow(e.getSentimentScore() - avgSentimentScore, 2))
            .average()
            .orElse(0.0);
        
        // Emotional stability: lower variance = higher stability
        double stability = Math.max(0, Math.min(100, 100 - (variance * 100)));
        
        // Calculate wellness score based on sentiment and consistency
        double wellnessScore = (avgSentimentScore * 50) + (stability * 0.5);
        wellnessScore = Math.max(0, Math.min(100, wellnessScore));
        
        // Generate pattern description
        String pattern = generatePatternDescription(sentimentCounts, avgSentimentScore, stability);
        
        result.put("pattern", pattern);
        result.put("dominantEmotion", dominantEmotion.substring(0, 1).toUpperCase() + dominantEmotion.substring(1));
        result.put("emotionalStability", String.format("%.0f%%", stability));
        result.put("wellnessScore", String.format("%.0f%%", wellnessScore));
        
        return result;
    }
    
    private String generatePatternDescription(Map<String, Long> sentimentCounts, double avgSentiment, double stability) {
        if (avgSentiment > 0.6 && stability > 70) {
            return "Consistently positive emotional state with good stability";
        } else if (avgSentiment > 0.6 && stability < 50) {
            return "Positive emotions with room for more consistency";
        } else if (avgSentiment < 0.4 && stability > 70) {
            return "Stable but experiencing challenging emotions";
        } else if (avgSentiment < 0.4 && stability < 50) {
            return "Experiencing emotional volatility - consider self-care";
        } else {
            return "Balanced emotional journey with natural fluctuations";
        }
    }
    
    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(@RequestParam(required = false) String username) {
        try {
            List<JournalEntry> entries;
            if (username != null && !username.trim().isEmpty()) {
                entries = journalRepository.findByUsernameOrderByCreatedAtDesc(username);
            } else {
                entries = journalRepository.findAllByOrderByCreatedAtDesc();
            }
            
            Map<String, Object> result = generateRealRecommendations(entries);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Recommendation generation failed: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    private Map<String, Object> generateRealRecommendations(List<JournalEntry> entries) {
        Map<String, Object> result = new HashMap<>();
        
        if (entries.isEmpty()) {
            result.put("recommendation", "Start journaling regularly to gain insights into your emotional patterns.");
            result.put("focusForTomorrow", "Write about one thing you're grateful for today.");
            result.put("generatedDate", java.time.LocalDate.now().toString());
            return result;
        }
        
        // Get recent entries for analysis
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<JournalEntry> recentEntries = entries.stream()
            .filter(e -> e.getCreatedAt().isAfter(weekAgo))
            .collect(Collectors.toList());
        
        // Analyze patterns for recommendations
        double avgSentiment = recentEntries.stream()
            .filter(e -> e.getSentimentScore() != null)
            .mapToDouble(JournalEntry::getSentimentScore)
            .average()
            .orElse(0.5);
        
        String recommendation = generatePersonalizedRecommendation(recentEntries, avgSentiment);
        String focusForTomorrow = generateTomorrowFocus(recentEntries);
        
        result.put("recommendation", recommendation);
        result.put("focusForTomorrow", focusForTomorrow);
        result.put("generatedDate", java.time.LocalDate.now().toString());
        
        return result;
    }
    
    private String generatePersonalizedRecommendation(List<JournalEntry> entries, double avgSentiment) {
        if (entries.size() < 3) {
            return "Try to journal more frequently to better track your emotional patterns and gain deeper insights.";
        }
        
        if (avgSentiment > 0.6) {
            return "Your consistently positive outlook is wonderful! Consider sharing your gratitude with others to spread the joy.";
        } else if (avgSentiment > 0.2) {
            return "You're maintaining a good emotional balance. Continue your current journaling practice for optimal wellness.";
        } else if (avgSentiment > -0.2) {
            return "Your emotions are fairly balanced. Try incorporating brief mindfulness moments during your day for greater clarity.";
        } else {
            return "You've been experiencing some challenges. Consider gentle self-care activities and reaching out to supportive friends.";
        }
    }
    
    private String generateTomorrowFocus(List<JournalEntry> entries) {
        // Analyze content themes
        List<String> contents = entries.stream()
            .map(JournalEntry::getContent)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        boolean mentionsWork = contents.stream().anyMatch(c -> c.toLowerCase().contains("work"));
        boolean mentionsStress = contents.stream().anyMatch(c -> c.toLowerCase().contains("stress") || c.toLowerCase().contains("anxious"));
        boolean mentionsGratitude = contents.stream().anyMatch(c -> c.toLowerCase().contains("grateful") || c.toLowerCase().contains("thank"));
        
        if (mentionsStress) {
            return "Take three deep breaths before your most challenging task tomorrow.";
        } else if (mentionsWork) {
            return "Schedule a 5-minute break during your workday for mental clarity.";
        } else if (!mentionsGratitude) {
            return "Write down three things you're grateful for before bed tonight.";
        } else {
            return "Notice and appreciate one small moment of joy during your day.";
        }
    }
    
    private String generateInsight(String sentiment, double score) {
        if (score > 0.6) {
            return "This entry shows very positive emotions. Keep up the great mood!";
        } else if (score > 0.2) {
            return "This entry has positive undertones. You're doing well!";
        } else if (score > -0.2) {
            return "This entry appears neutral. A balanced emotional state.";
        } else if (score > -0.6) {
            return "This entry shows some negative emotions. It's okay to have difficult days.";
        } else {
            return "This entry shows strong negative emotions. Consider reaching out for support if needed.";
        }
    }
}

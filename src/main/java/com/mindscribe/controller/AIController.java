package com.mindscribe.controller;

import com.mindscribe.service.SentimentAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    @Autowired
    private SentimentAnalysisService sentimentAnalysisService;

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

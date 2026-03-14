package com.mindscribe.controller;

import com.mindscribe.service.SentimentAnalysisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-status")
@CrossOrigin(origins = "*")
public class AIStatusController {

    private final SentimentAnalysisService sentimentAnalysisService;

    public AIStatusController(SentimentAnalysisService sentimentAnalysisService) {
        this.sentimentAnalysisService = sentimentAnalysisService;
    }

    @GetMapping("/status")
    public String getAIStatus() {
        boolean modelAvailable = sentimentAnalysisService.isModelAvailable();
        String error = sentimentAnalysisService.getLoadingError();
        return "{\"ai_model_loaded\": " + modelAvailable + ", \"error\": \"" + error + "\"}";
    }

    @PostMapping("/analyze")
    public String analyzeText(@RequestBody String text) {
        String sentiment = sentimentAnalysisService.analyzeSentiment(text);
        double score = sentimentAnalysisService.getSentimentScore(text);
        return "{\"text\": \"" + text + "\", \"sentiment\": \"" + sentiment + "\", \"score\": " + score + "}";
    }

    @PostMapping("/test-tokens")
    public String testTokenization(@RequestBody String text) {
        // This will help us see if the tokenizer is working properly
        String sentiment = sentimentAnalysisService.analyzeSentiment(text);
        return "{\"input\": \"" + text + "\", \"sentiment\": \"" + sentiment + "\"}";
    }
}

package com.mindscribe.controller;

import com.mindscribe.service.DiaryService;
import com.mindscribe.model.h2.JournalEntry;
import com.mindscribe.repository.h2.JournalRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diary")
@CrossOrigin(origins = "*")
public class DiaryController {

    private final DiaryService diaryService;
    private final JournalRepository journalRepository;

    public DiaryController(DiaryService diaryService, JournalRepository journalRepository) {
        this.diaryService = diaryService;
        this.journalRepository = journalRepository;
    }

    public record NewEntryRequest(String title, String content, String mood) {}

    @GetMapping("/health")
    public String health() {
        return "{\"status\":\"MindScribe Diary API OK!\"}";
    }

    @PostMapping("/entry")
    public JournalEntry createEntry(@RequestParam String username, @RequestBody NewEntryRequest request) {
        JournalEntry entry = diaryService.createEntry(request.title(), request.content(), username);
        
        // Use AI sentiment analysis but allow user mood override if provided
        if (request.mood() != null && !request.mood().isEmpty()) {
            entry.setSentiment(request.mood());
            // Set a default score based on mood
            double score = getMoodScore(request.mood());
            entry.setSentimentScore(score);
        }
        // AI analyzed sentiment is already set by DiaryService
        
        return journalRepository.save(entry);
    }
    
    private double getMoodScore(String mood) {
        return switch (mood.toLowerCase()) {
            case "happy" -> 0.8;
            case "excited" -> 0.9;
            case "sad" -> -0.6;
            case "anxious" -> -0.4;
            case "neutral" -> 0.0;
            default -> 0.0;
        };
    }

    @GetMapping("/entries")
    public List<JournalEntry> getEntries(@RequestParam String username) {
        if (username != null && !username.trim().isEmpty()) {
            return diaryService.getAllEntries(username);
        } else {
            return diaryService.getAllEntries(null);
        }
    }

    /**
     * Aggregated sentiment data for the Mood Calendar / Emotion Dashboard.
     * Returns a date -> {sentiment -> count} structure.
     */
    @GetMapping("/mood-calendar")
    public Map<LocalDate, Map<String, Long>> getMoodCalendar() {
        return diaryService.getMoodCalendar();
    }
}

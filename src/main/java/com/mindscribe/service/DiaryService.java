package com.mindscribe.service;

import com.mindscribe.model.h2.JournalEntry;
import com.mindscribe.repository.h2.JournalRepository;
import com.mindscribe.service.SentimentAnalysisService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DiaryService {

    private final JournalRepository journalRepository;
    private final SentimentAnalysisService sentimentAnalysisService;

    public DiaryService(JournalRepository journalRepository, SentimentAnalysisService sentimentAnalysisService) {
        this.journalRepository = journalRepository;
        this.sentimentAnalysisService = sentimentAnalysisService;
    }

    public JournalEntry createEntry(String title, String content, String username) {
        JournalEntry entry = new JournalEntry(title, content, username);
        
        // Use AI sentiment analysis
        String sentiment = sentimentAnalysisService.analyzeSentiment(content);
        Double sentimentScore = sentimentAnalysisService.getSentimentScore(content);
        
        entry.setSentiment(sentiment);
        entry.setSentimentScore(sentimentScore);
        
        return journalRepository.save(entry);
    }

    public List<JournalEntry> getAllEntries(String username) {
        if (username != null && !username.trim().isEmpty()) {
            return journalRepository.findByUsernameOrderByCreatedAtDesc(username);
        } else {
            return journalRepository.findAllByOrderByCreatedAtDesc();
        }
    }

    public Map<LocalDate, Map<String, Long>> getMoodCalendar() {
        List<Object[]> results = journalRepository.findSentimentCountsByDate();
        
        Map<LocalDate, Map<String, Long>> moodCalendar = new HashMap<>();
        
        for (Object[] result : results) {
            LocalDate date = ((java.sql.Date) result[0]).toLocalDate();
            String sentiment = (String) result[1];
            Long count = (Long) result[2];
            
            moodCalendar.computeIfAbsent(date, k -> new HashMap<>())
                       .put(sentiment, count);
        }
        
        return moodCalendar;
    }
}

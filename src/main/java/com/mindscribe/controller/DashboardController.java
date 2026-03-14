package com.mindscribe.controller;

import com.mindscribe.model.h2.JournalEntry;
import com.mindscribe.repository.h2.JournalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private JournalRepository journalRepository;

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats(@RequestParam(required = false) String username) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Get entries for specific user or all entries
            List<JournalEntry> allEntries;
            if (username != null && !username.trim().isEmpty()) {
                allEntries = journalRepository.findByUsernameOrderByCreatedAtDesc(username);
            } else {
                allEntries = journalRepository.findAllByOrderByCreatedAtDesc();
            }
            
            // Total entries
            stats.put("totalEntries", allEntries.size());
            
            // Current mood (from most recent entry with sentiment)
            String currentMood = "Neutral";
            if (!allEntries.isEmpty()) {
                JournalEntry mostRecent = allEntries.get(0);
                if (mostRecent.getSentiment() != null) {
                    currentMood = mostRecent.getSentiment();
                }
            }
            stats.put("currentMood", currentMood);
            
            // Day streak (consecutive days with entries)
            int dayStreak = calculateDayStreak(allEntries);
            stats.put("dayStreak", dayStreak);
            
            // Recent entry removed from home view
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to load dashboard stats");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    private int calculateDayStreak(List<JournalEntry> entries) {
        if (entries.isEmpty()) return 0;
        
        Set<String> daysWithEntries = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (JournalEntry entry : entries) {
            LocalDateTime entryDate = entry.getCreatedAt();
            // Only consider entries from last 30 days for streak
            if (ChronoUnit.DAYS.between(entryDate, now) <= 30) {
                String dayKey = entryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                daysWithEntries.add(dayKey);
            }
        }
        
        int streak = 0;
        LocalDateTime current = now.toLocalDate().atStartOfDay();
        
        // Count backwards from today
        while (true) {
            String dayKey = current.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            if (daysWithEntries.contains(dayKey)) {
                streak++;
                current = current.minusDays(1);
            } else {
                break;
            }
        }
        
        return streak;
    }
    
    private String getTimeAgo(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);
        
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else {
            long minutes = ChronoUnit.MINUTES.between(dateTime, now);
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        }
    }
}

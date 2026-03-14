package com.mindscribe.repository.h2;

import com.mindscribe.model.h2.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface JournalRepository extends JpaRepository<JournalEntry, Long> {
    
    List<JournalEntry> findByUsernameOrderByCreatedAtDesc(String username);
    
    List<JournalEntry> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT FUNCTION('DATE', e.createdAt) as date, e.sentiment, COUNT(e) as count " +
           "FROM JournalEntry e " +
           "WHERE e.sentiment IS NOT NULL " +
           "GROUP BY FUNCTION('DATE', e.createdAt), e.sentiment")
    List<Object[]> findSentimentCountsByDate();
}

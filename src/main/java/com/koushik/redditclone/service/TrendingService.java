package com.koushik.redditclone.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.koushik.redditclone.model.Post;
import com.koushik.redditclone.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class TrendingService {
    
    private final PostRepository postRepository;
    private final Map<String, Double> trendingHashtags = new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void calculateTrends() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minus(24, ChronoUnit.HOURS);
        List<Post> recentPosts = postRepository.findByTimestampAfter(twentyFourHoursAgo);
        
        Map<String, Double> hashtagScores = new HashMap<>();
        
        for (Post post : recentPosts) {
            // Calculate time-based weight (1.0 for newest posts, decreasing to 0.0 for 24h old posts)
            double hoursAgo = ChronoUnit.HOURS.between(post.getTimestamp(), LocalDateTime.now());
            double timeWeight = Math.max(0.0, 1.0 - (hoursAgo / 24.0));
            
            for (String hashtag : post.getHashtags()) {
                hashtagScores.merge(hashtag, timeWeight, Double::sum);
            }
        }
        
        // Sort hashtags by score and keep top 10
        List<Map.Entry<String, Double>> sortedHashtags = new ArrayList<>(hashtagScores.entrySet());
        sortedHashtags.sort(Map.Entry.<String, Double>comparingByValue().reversed());
        
        // Update trending hashtags
        trendingHashtags.clear();
        sortedHashtags.stream()
            .limit(10)
            .forEach(entry -> trendingHashtags.put(entry.getKey(), entry.getValue()));
    }
    
    public List<String> getTrendingHashtags() {
        return new ArrayList<>(trendingHashtags.keySet());
    }
}

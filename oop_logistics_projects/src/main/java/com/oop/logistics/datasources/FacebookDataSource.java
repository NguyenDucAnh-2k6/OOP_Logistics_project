package com.oop.logistics.datasources;

import com.oop.logistics.core.DataSource;
import com.oop.logistics.core.SourceConfiguration;
import com.oop.logistics.Facebook.FacebookService;
import com.oop.logistics.models.DisasterEvent;
import java.util.List;
import java.util.ArrayList;

/**
 * Facebook implementation of DataSource interface
 */
public class FacebookDataSource implements DataSource {
    
    private FacebookService facebookService;
    private String pageId;
    private boolean configured;
    
    public FacebookDataSource() {
        this.configured = false;
    }
    
    @Override
    public void configure(SourceConfiguration config) {
        String accessToken = config.getStringProperty("access_token");
        this.pageId = config.getStringProperty("page_id");
        
        if (accessToken != null && pageId != null) {
            this.facebookService = new FacebookService(accessToken);
            this.configured = true;
        } else {
            throw new IllegalArgumentException(
                "Facebook requires 'access_token' and 'page_id' in configuration");
        }
    }
    
    @Override
    public List<DisasterEvent> fetchDisasterEvents() {
        if (!isAvailable()) {
            return new ArrayList<>();
        }
        return facebookService.extractDisasterEvents(pageId);
    }
    
    @Override
    public List<DisasterEvent> fetchViralEvents(int minEngagement) {
        if (!isAvailable()) {
            return new ArrayList<>();
        }
        
        return facebookService.getViralDisasterPosts(pageId, minEngagement)
            .stream()
            .map(post -> {
                DisasterEvent event = new DisasterEvent();
                event.setSourceId(post.getId());
                event.setDescription(post.getMessage());
                event.setTimestamp(post.getCreatedTime());
                event.setSource("Facebook");
                event.setEngagement(post.getLikes() + post.getComments() + post.getShares());
                return event;
            })
            .toList();
    }
    
    @Override
    public String getSourceName() {
        return "Facebook";
    }
    
    @Override
    public boolean isAvailable() {
        return configured && facebookService != null;
    }
}
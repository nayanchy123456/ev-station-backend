package com.evstation.ev_charging_backend.config;

import com.evstation.ev_charging_backend.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ENHANCED WebSocket Event Listener
 * 
 * ✅ IMPROVEMENTS:
 * 1. Tracks active sessions per user
 * 2. Handles multi-device connections
 * 3. Broadcasts presence changes
 * 4. Delivers pending messages on connect
 * 5. Better error handling
 * 6. Connection monitoring
 * 
 * Events Handled:
 * - SessionConnectEvent: Initial connection attempt
 * - SessionConnectedEvent: Successful connection established
 * - SessionSubscribeEvent: User subscribes to destination
 * - SessionDisconnectEvent: Connection closed
 * 
 * Presence Logic:
 * - User is ONLINE if they have at least one active session
 * - User is OFFLINE only when all sessions are disconnected
 * - Supports multiple devices/tabs simultaneously
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Track active sessions per user (userId -> Set of sessionIds)
    // Supports multi-device connections
    private final Map<Long, ConcurrentHashMap<String, Long>> userSessions = new ConcurrentHashMap<>();
    
    /**
     * Handle Initial Connection Event
     * 
     * ✅ ENHANCED: Logs connection attempts for monitoring
     * 
     * Triggered: When client initiates WebSocket connection (before authentication)
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.info("🔌 WebSocket connection attempt - Session: {}", sessionId);
        
        // User info not available yet (authentication happens in interceptor)
        // This event is mainly for monitoring connection attempts
    }
    
    /**
     * Handle Successful Connection Event
     * 
     * ✅ ENHANCED: 
     * - Tracks user sessions
     * - Sets user ONLINE (if first session)
     * - Delivers pending messages
     * - Broadcasts presence update
     * 
     * Triggered: After authentication succeeds and connection is fully established
     */
    @EventListener
    public void handleWebSocketConnectedListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // Get user ID from session (set during authentication)
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        
        if (userId == null) {
            log.warn("⚠️ WebSocket connected without userId - Session: {}", sessionId);
            return;
        }
        
        log.info("✅ WebSocket connected successfully - User: {} (ID: {}), Session: {}", 
                 username, userId, sessionId);
        
        try {
            // Track this session for the user
            boolean isFirstSession = addUserSession(userId, sessionId);
            
            // If this is the user's first active session, mark them ONLINE
            if (isFirstSession) {
                log.info("👋 User {} came ONLINE (first session)", userId);
                chatMessageService.setUserOnline(userId);
                
                // Deliver any pending messages (sent while user was offline)
                int deliveredCount = chatMessageService.markPendingMessagesAsDelivered(userId);
                if (deliveredCount > 0) {
                    log.info("📬 Delivered {} pending messages to user {}", deliveredCount, userId);
                }
                
                // Broadcast presence update to all conversations
                broadcastPresenceUpdate(userId, "ONLINE");
            } else {
                log.info("📱 Additional session for user {} (multi-device)", userId);
            }
            
        } catch (Exception e) {
            log.error("❌ Error handling WebSocket connection for user {} - Session: {} - Error: {}", 
                     userId, sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * Handle Subscription Event
     * 
     * ✅ NEW: Track what destinations users subscribe to
     * Useful for debugging and monitoring
     * 
     * Triggered: When client subscribes to a destination (e.g., /topic/messages/123)
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        
        if (destination != null && userId != null) {
            log.debug("📡 User {} subscribed to: {} - Session: {}", userId, destination, sessionId);
        }
    }
    
    /**
     * Handle Disconnection Event
     * 
     * ✅ ENHANCED:
     * - Tracks session removal
     * - Sets user OFFLINE only if last session
     * - Broadcasts presence update
     * - Cleans up resources
     * 
     * Triggered: When WebSocket connection is closed (graceful or timeout)
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        
        if (userId == null) {
            log.warn("⚠️ WebSocket disconnected without userId - Session: {}", sessionId);
            return;
        }
        
        log.info("🔌 WebSocket disconnected - User: {} (ID: {}), Session: {}", 
                 username, userId, sessionId);
        
        try {
            // Remove this session from user's session list
            boolean isLastSession = removeUserSession(userId, sessionId);
            
            // If this was the user's last active session, mark them OFFLINE
            if (isLastSession) {
                log.info("👋 User {} went OFFLINE (last session closed)", userId);
                chatMessageService.setUserOffline(userId);
                
                // Broadcast presence update to all conversations
                broadcastPresenceUpdate(userId, "OFFLINE");
            } else {
                log.info("📱 Session closed for user {}, but other sessions remain active", userId);
            }
            
        } catch (Exception e) {
            log.error("❌ Error handling WebSocket disconnection for user {} - Session: {} - Error: {}", 
                     userId, sessionId, e.getMessage(), e);
        }
    }
    
    // ==================== SESSION MANAGEMENT ====================
    
    /**
     * Add User Session to Tracking Map
     * 
     * @param userId User ID
     * @param sessionId Session ID
     * @return true if this is the user's first session, false if they had other sessions
     */
    private boolean addUserSession(Long userId, String sessionId) {
        ConcurrentHashMap<String, Long> sessions = userSessions.computeIfAbsent(
            userId, 
            k -> new ConcurrentHashMap<>()
        );
        
        sessions.put(sessionId, System.currentTimeMillis());
        
        // Return true if this is the only session (user just came online)
        return sessions.size() == 1;
    }
    
    /**
     * Remove User Session from Tracking Map
     * 
     * @param userId User ID
     * @param sessionId Session ID
     * @return true if this was the user's last session, false if they have other sessions
     */
    private boolean removeUserSession(Long userId, String sessionId) {
        ConcurrentHashMap<String, Long> sessions = userSessions.get(userId);
        
        if (sessions == null) {
            return true; // No sessions tracked, consider as last session
        }
        
        sessions.remove(sessionId);
        
        // If no more sessions, remove user from tracking map
        if (sessions.isEmpty()) {
            userSessions.remove(userId);
            return true; // This was the last session
        }
        
        return false; // User still has other active sessions
    }
    
    /**
     * Get Active Session Count for User
     * 
     * @param userId User ID
     * @return Number of active sessions
     */
    public int getActiveSessionCount(Long userId) {
        ConcurrentHashMap<String, Long> sessions = userSessions.get(userId);
        return sessions == null ? 0 : sessions.size();
    }
    
    /**
     * Check if User Has Any Active Sessions
     * 
     * @param userId User ID
     * @return true if user has at least one active session
     */
    public boolean isUserConnected(Long userId) {
        return getActiveSessionCount(userId) > 0;
    }
    
    // ==================== PRESENCE BROADCASTING ====================
    
    /**
     * Broadcast Presence Update to User's Conversations
     * 
     * ✅ NEW: Notifies all conversation participants when user comes online/offline
     * 
     * @param userId User whose presence changed
     * @param status New presence status ("ONLINE" or "OFFLINE")
     */
    private void broadcastPresenceUpdate(Long userId, String status) {
        try {
            // Create presence update message
            Map<String, Object> presenceUpdate = Map.of(
                "userId", userId,
                "status", status,
                "timestamp", System.currentTimeMillis()
            );
            
            // Broadcast to topic so all clients can update UI
            messagingTemplate.convertAndSend(
                "/topic/presence/" + userId,
                presenceUpdate
            );
            
            log.debug("📢 Broadcasted presence update: User {} is {}", userId, status);
            
        } catch (Exception e) {
            log.error("❌ Failed to broadcast presence update for user {}: {}", 
                     userId, e.getMessage());
        }
    }
    
    // ==================== MONITORING ====================
    
    /**
     * Get Total Active Connections
     * 
     * @return Total number of active WebSocket connections
     */
    public int getTotalActiveConnections() {
        return userSessions.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
    
    /**
     * Get Total Active Users
     * 
     * @return Number of unique users with active connections
     */
    public int getTotalActiveUsers() {
        return userSessions.size();
    }
    
    /**
     * Get Connection Statistics
     * 
     * @return Map with connection statistics
     */
    public Map<String, Object> getConnectionStats() {
        return Map.of(
            "totalUsers", getTotalActiveUsers(),
            "totalConnections", getTotalActiveConnections(),
            "averageSessionsPerUser", getTotalActiveUsers() > 0 
                ? (double) getTotalActiveConnections() / getTotalActiveUsers() 
                : 0.0
        );
    }
}
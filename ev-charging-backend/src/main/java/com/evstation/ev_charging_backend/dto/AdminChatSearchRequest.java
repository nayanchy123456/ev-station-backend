package com.evstation.ev_charging_backend.dto;

import com.evstation.ev_charging_backend.enums.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * DTO for admin to search users/hosts to initiate chat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminChatSearchRequest {
    
    /**
     * Search term (name, email, phone)
     */
    private String searchTerm;
    
    /**
     * Filter by role (USER, HOST)
     * Optional - if not specified, searches both
     */
    private Role roleFilter;
    
    /**
     * Page number (0-indexed)
     */
    @Builder.Default
    private Integer page = 0;
    
    /**
     * Page size
     */
    @Builder.Default
    private Integer size = 20;
}
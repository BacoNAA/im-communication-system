package com.im.imcommunicationsystem.group.entity;

import com.im.imcommunicationsystem.group.enums.PollStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群投票实体类
 */
@Data
@Entity
@Table(name = "polls")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Poll {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "group_id", nullable = false)
    private Long groupId;
    
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "is_multiple", nullable = false)
    private Boolean isMultiple;
    
    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PollStatus status;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollOption> options;
} 
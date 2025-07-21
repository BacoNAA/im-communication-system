package com.im.imcommunicationsystem.group.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 投票记录实体类
 */
@Data
@Entity
@Table(name = "poll_votes")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollVote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "poll_id", nullable = false, insertable = false, updatable = false)
    private Long pollId;
    
    @Column(name = "option_id", nullable = false, insertable = false, updatable = false)
    private Long optionId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @CreationTimestamp
    @Column(name = "voted_at", nullable = false, updatable = false)
    private LocalDateTime votedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    @JsonIgnore
    private Poll poll;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    @JsonIgnore
    private PollOption option;
} 
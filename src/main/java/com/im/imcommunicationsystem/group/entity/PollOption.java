package com.im.imcommunicationsystem.group.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 投票选项实体类
 */
@Data
@Entity
@Table(name = "poll_options")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollOption {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "poll_id", nullable = false, insertable = false, updatable = false)
    private Long pollId;
    
    @Column(name = "option_text", nullable = false)
    private String optionText;
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    @JsonIgnore
    private Poll poll;
    
    @OneToMany(mappedBy = "option", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PollVote> votes;
} 
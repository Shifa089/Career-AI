package com.careerai.interview.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Comprehensive final feedback for a completed {@link InterviewSession}. JSON-array fields
 * (strong/improvement areas, recommended resources) are persisted as JSON strings in TEXT columns
 * and expanded back into lists by the mapper.
 */
@Entity
@Table(name = "interview_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private InterviewSession session;

    @Column(name = "technical_score")
    private Integer technicalScore;

    @Column(name = "behavioural_score")
    private Integer behaviouralScore;

    @Column(name = "communication_score")
    private Integer communicationScore;

    @Column(name = "problem_solving_score")
    private Integer problemSolvingScore;

    @Column(name = "strong_areas", columnDefinition = "TEXT")
    private String strongAreas;

    @Column(name = "improvement_areas", columnDefinition = "TEXT")
    private String improvementAreas;

    @Column(name = "detailed_feedback", columnDefinition = "TEXT")
    private String detailedFeedback;

    @Column(name = "recommended_resources", columnDefinition = "TEXT")
    private String recommendedResources;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    void onCreate() {
        this.generatedAt = LocalDateTime.now();
    }
}

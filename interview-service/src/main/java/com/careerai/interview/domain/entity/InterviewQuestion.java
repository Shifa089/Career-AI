package com.careerai.interview.domain.entity;

import com.careerai.interview.domain.enums.QuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single question asked during an {@link InterviewSession}, along with the candidate's answer
 * and Claude's per-answer evaluation.
 */
@Entity
@Table(name = "interview_questions",
        indexes = @Index(name = "idx_interview_questions_session_id", columnList = "session_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QuestionType type;

    @Column(length = 10)
    private String difficulty;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    @Column(name = "answer_score")
    private Integer answerScore;

    @Column(name = "answer_feedback", columnDefinition = "TEXT")
    private String answerFeedback;

    @Column(name = "ideal_answer", columnDefinition = "TEXT")
    private String idealAnswer;

    @Column(name = "skills_tested", columnDefinition = "TEXT")
    private String skillsTested;

    @Column(name = "asked_at")
    private LocalDateTime askedAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
}

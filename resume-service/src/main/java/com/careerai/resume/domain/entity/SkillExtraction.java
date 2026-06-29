package com.careerai.resume.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A single skill inferred from a resume by the AI analysis, attached to a {@link ResumeAnalysis}.
 */
@Entity
@Table(name = "skill_extractions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillExtraction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private ResumeAnalysis analysis;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Column(length = 30)
    private String category;

    @Column(name = "proficiency_level", length = 20)
    private String proficiencyLevel;

    @Column(name = "years_used")
    private Integer yearsUsed;

    @Column(name = "inferred_from_context", nullable = false)
    @Builder.Default
    private boolean inferredFromContext = false;
}

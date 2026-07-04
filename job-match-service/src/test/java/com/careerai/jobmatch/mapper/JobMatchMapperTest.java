package com.careerai.jobmatch.mapper;

import com.careerai.jobmatch.domain.entity.JobListing;
import com.careerai.jobmatch.domain.entity.JobMatch;
import com.careerai.jobmatch.domain.enums.ExperienceLevel;
import com.careerai.jobmatch.domain.enums.JobType;
import com.careerai.jobmatch.domain.enums.MatchStatus;
import com.careerai.jobmatch.dto.ai.SkillGapResult;
import com.careerai.jobmatch.dto.response.JobListingResponse;
import com.careerai.jobmatch.dto.response.JobMatchResponse;
import com.careerai.jobmatch.dto.response.LearningPathResponse;
import com.careerai.jobmatch.dto.response.SkillGapResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JobMatchMapperTest {

    private JobMatchMapper mapper;

    @BeforeEach
    void setUp() {
        // Use the MapStruct-generated implementation, wiring in the shared ObjectMapper used for
        // JSON-array TEXT expansion.
        JobMatchMapperImpl impl = new JobMatchMapperImpl();
        impl.objectMapper = new ObjectMapper();
        this.mapper = impl;
    }

    @Test
    void toJobListingResponse_expandsSkillJsonAndEnums() {
        JobListing listing = JobListing.builder()
                .id(UUID.randomUUID())
                .title("Backend Engineer").company("Acme").location("Remote")
                .jobType(JobType.REMOTE).experienceLevel(ExperienceLevel.SENIOR)
                .descriptionText("desc")
                .requiredSkills("[\"Java\",\"Kafka\"]")
                .niceToHaveSkills("[\"Go\"]")
                .salaryRange("$100k").sourceUrl("http://x")
                .build();

        JobListingResponse response = mapper.toJobListingResponse(listing);

        assertThat(response.requiredSkills()).containsExactly("Java", "Kafka");
        assertThat(response.niceToHaveSkills()).containsExactly("Go");
        assertThat(response.jobType()).isEqualTo("REMOTE");
        assertThat(response.experienceLevel()).isEqualTo("SENIOR");
    }

    @Test
    void toJobListingResponse_handlesBlankSkillJson() {
        JobListing listing = JobListing.builder()
                .id(UUID.randomUUID()).title("t").company("c").descriptionText("d").build();

        JobListingResponse response = mapper.toJobListingResponse(listing);

        assertThat(response.requiredSkills()).isEmpty();
        assertThat(response.niceToHaveSkills()).isEmpty();
    }

    @Test
    void toJobMatchResponse_mapsIdAndNestedJob() {
        JobListing listing = JobListing.builder().id(UUID.randomUUID()).title("t").company("c")
                .descriptionText("d").requiredSkills("[\"Java\"]").build();
        UUID matchId = UUID.randomUUID();
        JobMatch match = JobMatch.builder()
                .id(matchId).jobListing(listing).similarityScore(0.88).matchPercentage(88)
                .matchedSkills("[\"Java\"]").missingSkills("[\"Kafka\"]").status(MatchStatus.SAVED)
                .build();

        JobMatchResponse response = mapper.toJobMatchResponse(match);

        assertThat(response.matchId()).isEqualTo(matchId);
        assertThat(response.job().title()).isEqualTo("t");
        assertThat(response.matchedSkills()).containsExactly("Java");
        assertThat(response.missingSkills()).containsExactly("Kafka");
        assertThat(response.status()).isEqualTo("SAVED");
    }

    @Test
    void toSkillGapResponse_attachesTitleAndPartials() {
        SkillGapResult result = new SkillGapResult(
                List.of("Java"), List.of("Kafka"),
                List.of(new SkillGapResult.PartialMatch("Spring", "basic", "advanced")),
                65, "NEEDS_WORK", List.of(), "summary");

        SkillGapResponse response = mapper.toSkillGapResponse("Backend Engineer", result);

        assertThat(response.jobTitle()).isEqualTo("Backend Engineer");
        assertThat(response.partialMatches()).hasSize(1);
        assertThat(response.partialMatches().get(0).skill()).isEqualTo("Spring");
        assertThat(response.gapScore()).isEqualTo(65);
    }

    @Test
    void toLearningPathResponse_sumsEstimatedWeeks() {
        SkillGapResult result = new SkillGapResult(
                List.of(), List.of(), List.of(), 50, "NEEDS_WORK",
                List.of(
                        new SkillGapResult.LearningPathItem("Kafka", "HIGH", 4,
                                List.of(new SkillGapResult.LearningResource("Kafka 101", "http://x", "COURSE"))),
                        new SkillGapResult.LearningPathItem("Go", "LOW", 6, null)),
                "summary");

        LearningPathResponse response = mapper.toLearningPathResponse("Backend Engineer", result);

        assertThat(response.totalEstimatedWeeks()).isEqualTo(10);
        assertThat(response.prioritizedSkills()).hasSize(2);
        assertThat(response.prioritizedSkills().get(0).resources()).hasSize(1);
        assertThat(response.prioritizedSkills().get(1).resources()).isEmpty();
    }
}

package com.careerai.jobmatch.repository;

import com.careerai.jobmatch.domain.entity.JobListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JobListingRepository extends JpaRepository<JobListing, UUID> {

    boolean existsByExternalId(String externalId);

    List<JobListing> findByActiveTrue();

    Page<JobListing> findByEmployerIdOrderByCreatedAtDesc(UUID employerId, Pageable pageable);

    java.util.Optional<JobListing> findByIdAndEmployerId(UUID id, UUID employerId);

    /**
     * Free-text search over active listings by keyword (title/company/description) and location. A
     * {@code null} filter is treated as "no constraint".
     */
    @Query("""
            SELECT j FROM JobListing j
            WHERE j.active = true
              AND (:keyword IS NULL
                   OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(j.company) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(j.descriptionText) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
            """)
    Page<JobListing> search(@Param("keyword") String keyword,
                            @Param("location") String location,
                            Pageable pageable);
}

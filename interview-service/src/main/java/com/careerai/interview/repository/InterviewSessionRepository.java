package com.careerai.interview.repository;

import com.careerai.interview.domain.entity.InterviewSession;
import com.careerai.interview.domain.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    Page<InterviewSession> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, SessionStatus status);

    @Query("select avg(s.overallScore) from InterviewSession s "
            + "where s.userId = :userId and s.status = :status and s.overallScore is not null")
    Double averageOverallScore(@Param("userId") UUID userId, @Param("status") SessionStatus status);
}

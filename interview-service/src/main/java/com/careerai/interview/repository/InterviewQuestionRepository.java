package com.careerai.interview.repository;

import com.careerai.interview.domain.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {

    List<InterviewQuestion> findBySessionIdOrderByQuestionNumber(UUID sessionId);

    Optional<InterviewQuestion> findBySessionIdAndQuestionNumber(UUID sessionId, int questionNumber);
}

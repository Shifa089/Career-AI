package com.careerai.resume.repository;

import com.careerai.resume.domain.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    List<Resume> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Resume> findByUserIdAndIsPrimaryTrue(UUID userId);
}

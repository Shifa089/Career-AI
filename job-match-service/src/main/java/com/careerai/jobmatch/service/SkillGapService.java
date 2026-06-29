package com.careerai.jobmatch.service;

import com.careerai.jobmatch.dto.ai.SkillGapResult;

import java.util.List;

/**
 * Claude-backed skill-gap analysis between a candidate's skills and a job's requirements.
 */
public interface SkillGapService {

    SkillGapResult analyseSkillGap(List<String> resumeSkills, List<String> requiredSkills, String jobTitle);
}

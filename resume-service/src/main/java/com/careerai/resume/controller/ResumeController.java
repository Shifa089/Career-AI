package com.careerai.resume.controller;

import com.careerai.common.dto.ApiResponse;
import com.careerai.resume.dto.response.ResumeAnalysisResponse;
import com.careerai.resume.dto.response.ResumeResponse;
import com.careerai.resume.security.AuthenticatedUser;
import com.careerai.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Resume upload, retrieval, analysis, and lifecycle endpoints. The caller's identity is taken from
 * the gateway-forwarded {@code X-User-*} headers (see {@code HeaderAuthenticationFilter}).
 */
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
@Tag(name = "Resumes", description = "Upload, analyse, and manage resumes")
public class ResumeController {

    private final ResumeService resumeService;

    @Operation(summary = "Upload a resume and trigger asynchronous AI analysis")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResumeResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "targetRole", required = false) String targetRole) {
        AuthenticatedUser user = AuthenticatedUser.current();
        ResumeResponse response = resumeService.uploadAndAnalyse(file, user.userId(), user.email(), targetRole);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Resume uploaded; analysis in progress", response));
    }

    @Operation(summary = "List the current user's resumes")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> list() {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success(resumeService.getResumesByUser(user.userId())));
    }

    @Operation(summary = "Get a single resume")
    @GetMapping("/{resumeId}")
    public ResponseEntity<ApiResponse<ResumeResponse>> get(@PathVariable UUID resumeId) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success(resumeService.getResume(resumeId, user.userId())));
    }

    @Operation(summary = "Get the AI analysis for a resume")
    @GetMapping("/{resumeId}/analysis")
    public ResponseEntity<ApiResponse<ResumeAnalysisResponse>> getAnalysis(@PathVariable UUID resumeId) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success(resumeService.getAnalysis(resumeId, user.userId())));
    }

    @Operation(summary = "Delete a resume and its stored file")
    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> delete(@PathVariable UUID resumeId) {
        AuthenticatedUser user = AuthenticatedUser.current();
        resumeService.deleteResume(resumeId, user.userId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark a resume as the user's primary")
    @PatchMapping("/{resumeId}/primary")
    public ResponseEntity<ApiResponse<ResumeResponse>> setPrimary(@PathVariable UUID resumeId) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return ResponseEntity.ok(ApiResponse.success("Primary resume updated",
                resumeService.setPrimary(resumeId, user.userId())));
    }

    @Operation(summary = "Redirect to a presigned download URL for the resume file")
    @GetMapping("/{resumeId}/download")
    public ResponseEntity<Void> download(@PathVariable UUID resumeId) {
        AuthenticatedUser user = AuthenticatedUser.current();
        String url = resumeService.getDownloadUrl(resumeId, user.userId());
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}

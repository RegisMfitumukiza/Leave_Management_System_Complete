package com.daking.leave.controller;

import com.daking.leave.dto.response.DocumentResponse;
import com.daking.leave.service.interfaces.DocumentService;
import com.daking.auth.api.dto.UserResponseDTO;
import com.daking.leave.client.UserInfoClient;
import com.daking.leave.model.Document;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {
    private final DocumentService documentService;
    private final UserInfoClient userInfoClient;

    /**
     * Upload a document for the current user
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> uploadDocument(@AuthenticationPrincipal String userEmail,
            @RequestParam("file") @NotNull MultipartFile file) {
        log.info("Received upload request: userEmail={}, fileName={}, size={}", userEmail,
                file != null ? file.getOriginalFilename() : null, file != null ? file.getSize() : null);
        if (userEmail == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            // Lookup user by email (no token needed)
            UserResponseDTO user = userInfoClient.getUserByEmail(userEmail);
            if (user == null) {
                log.warn("User not found for email: {}", userEmail);
                return ResponseEntity.status(404).build();
            }
            DocumentResponse response = documentService.uploadDocument(user.getId(), file);
            log.info("Document uploaded successfully for user {}", user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to upload document for user {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get a document by its ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') or @documentSecurity.isOwner(#id, principal)")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        try {
            DocumentResponse response = documentService.getDocumentById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch document by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(404).build();
        }
    }

    /**
     * Get all documents for a user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') or #userId == principal")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByUser(@PathVariable Long userId) {
        try {
            List<DocumentResponse> docs = documentService.getDocumentsByUser(userId);
            return ResponseEntity.ok(docs);
        } catch (Exception e) {
            log.error("Failed to fetch documents for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Download a document file by filename
     */
    @GetMapping("/download/{filename}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') or @documentSecurity.canAccessFile(#filename, principal)")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("./uploads/documents").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                log.warn("File not found: {}", filename);
                return ResponseEntity.notFound().build();
            }
            // Lookup file type from DB (refactored: use service method if available)
            String contentType = "application/octet-stream";
            Document doc = null;
            try {
                doc = documentService.getDocumentByFileName(filename);
                if (doc != null && doc.getFileType() != null && !doc.getFileType().isEmpty()) {
                    contentType = doc.getFileType();
                }
            } catch (Exception e) {
                // fallback to octet-stream
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            log.error("Malformed file URL for filename {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.status(400).build();
        } catch (Exception e) {
            log.error("Failed to download document {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Approve a document
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<DocumentResponse> approveDocument(@PathVariable Long id) {
        try {
            DocumentResponse doc = documentService.updateDocumentStatus(id, "APPROVED");
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            log.error("Failed to approve document {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Reject a document
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<DocumentResponse> rejectDocument(@PathVariable Long id) {
        try {
            DocumentResponse doc = documentService.updateDocumentStatus(id, "REJECTED");
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            log.error("Failed to reject document {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get documents for the current user (by userId param)
     */
    @GetMapping("/my-documents")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<DocumentResponse>> getMyDocuments(@RequestParam Long userId) {
        try {
            List<DocumentResponse> docs = documentService.getDocumentsByUserId(userId);
            return ResponseEntity.ok(docs);
        } catch (Exception e) {
            log.error("Failed to fetch my documents for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Legacy support: /api/documents/leave/{id} -> /api/documents/{id}
     */
    @GetMapping("/leave/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') or @documentSecurity.isOwner(#id, principal)")
    public ResponseEntity<DocumentResponse> getDocumentByIdLegacy(@PathVariable Long id) {
        return getDocumentById(id);
    }
}
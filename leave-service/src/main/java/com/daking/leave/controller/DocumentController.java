package com.daking.leave.controller;

import com.daking.leave.dto.response.DocumentResponse;
import com.daking.leave.service.interfaces.DocumentService;
import com.daking.auth.api.dto.UserResponseDTO;
import com.daking.leave.client.UserInfoClient;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.daking.leave.model.Document;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    private final UserInfoClient userInfoClient;
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    // Upload document
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> uploadDocument(@AuthenticationPrincipal String userEmail,
            @RequestParam("file") @NotNull MultipartFile file) {
        logger.info("Received upload request: userEmail={}, fileName={}, size={}", userEmail,
                file != null ? file.getOriginalFilename() : null, file != null ? file.getSize() : null);
        if (userEmail == null) {
            return ResponseEntity.status(401).build();
        }
        // Lookup user by email
        UserResponseDTO user = userInfoClient.getUserByEmail(userEmail, null);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }
        return ResponseEntity.ok(documentService.uploadDocument(user.getId(), file));
    }

    // Get document by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') or @documentSecurity.isOwner(#id, principal)")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    // Get documents by user
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') or #userId == principal")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(documentService.getDocumentsByUser(userId));
    }

    // Download document file
    @GetMapping("/download/{filename}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') or @documentSecurity.canAccessFile(#filename, principal)")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String filename) throws MalformedURLException {
        // This assumes files are stored in ./uploads/documents
        Path filePath = Paths.get("./uploads/documents").resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            throw new IllegalArgumentException("File not found");
        }
        // Lookup file type from DB
        String contentType = "application/octet-stream";
        try {
            Document doc = documentService instanceof com.daking.leave.service.impl.DocumentServiceImpl
                    ? ((com.daking.leave.service.impl.DocumentServiceImpl) documentService).getDocumentRepository()
                            .findByFileName(filename)
                    : null;
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
    }

    // Approve document
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<DocumentResponse> approveDocument(@PathVariable Long id) {
        DocumentResponse doc = documentService.updateDocumentStatus(id, "APPROVED");
        return ResponseEntity.ok(doc);
    }

    // Reject document
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<DocumentResponse> rejectDocument(@PathVariable Long id) {
        DocumentResponse doc = documentService.updateDocumentStatus(id, "REJECTED");
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/my-documents")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<DocumentResponse>> getMyDocuments(@RequestParam Long userId) {
        List<DocumentResponse> docs = documentService.getDocumentsByUserId(userId);
        return ResponseEntity.ok(docs);
    }

    // Legacy support: /api/documents/leave/{id} -> /api/documents/{id}
    @GetMapping("/leave/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') or @documentSecurity.isOwner(#id, principal)")
    public ResponseEntity<DocumentResponse> getDocumentByIdLegacy(@PathVariable Long id) {
        // Delegate to the main method
        return getDocumentById(id);
    }
}
package com.daking.leave.security;

import com.daking.leave.model.Document;
import com.daking.leave.repository.DocumentRepository;
import com.daking.leave.client.UserInfoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.util.Optional;

@Component
public class DocumentSecurity {
    private final DocumentRepository documentRepository;
    private final UserInfoClient userInfoClient;

    @Autowired
    public DocumentSecurity(DocumentRepository documentRepository, UserInfoClient userInfoClient) {
        this.documentRepository = documentRepository;
        this.userInfoClient = userInfoClient;
    }

    /**
     * Checks if the document with the given ID belongs to the user with the given
     * username (userId).
     */
    public boolean isOwner(Long documentId, String username) {
        Long userId = parseUserId(username);
        if (userId == null)
            return false;
        Optional<Document> docOpt = documentRepository.findById(documentId);
        return docOpt.map(doc -> doc.getUserId().equals(userId)).orElse(false);
    }

    /**
     * Checks if the user with the given username (userId) can access the file with
     * the given filename.
     * Allows access if the user is the owner, or if the user is a manager/admin.
     */
    public boolean canAccessFile(String filename, String username) {
        Long userId = parseUserId(username);
        if (userId == null)
            return false;
        Optional<Document> docOpt = documentRepository.findAll().stream()
                .filter(doc -> doc.getFileName().equals(filename))
                .findFirst();
        if (docOpt.isEmpty())
            return false;
        Document doc = docOpt.get();
        if (doc.getUserId().equals(userId))
            return true;
        String token = getCurrentToken();
        if (token == null)
            return false;
        String role = userInfoClient.getUserRole(userId, token);
        if (role == null)
            return false;
        if ("ADMIN".equals(role))
            return true;
        if ("MANAGER".equals(role)) {
            // Get departments managed by this manager
            java.util.List<Long> managedDepartments = userInfoClient.getDepartmentsManaged(userId, token);
            // Get document owner's department
            com.daking.auth.api.model.User owner = userInfoClient.getUserById(doc.getUserId(), token);
            Long ownerDept = owner != null ? owner.getDepartmentId() : null;
            if (ownerDept != null && managedDepartments != null && managedDepartments.contains(ownerDept)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper to parse userId from username (assuming username is userId as String).
     */
    private Long parseUserId(String username) {
        try {
            return Long.valueOf(username);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Helper to get the current JWT token from the SecurityContext.
     */
    private String getCurrentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null)
            return null;
        if (authentication.getCredentials() instanceof String credentials) {
            return "Bearer " + credentials;
        }
        return null;
    }
}
package com.daking.leave.security;

import com.daking.auth.api.dto.UserResponseDTO;
import com.daking.leave.model.Document;
import com.daking.leave.repository.DocumentRepository;
import com.daking.leave.client.UserInfoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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

        return documentRepository.findById(documentId)
                .map(doc -> doc.getUserId().equals(userId))
                .orElse(false);
    }

    /**
     * Checks if the user with the given username (userId) can access the file with
     * the given filename.
     * Allows access if the user is the owner, or if the user is a manager/admin.
     */
    public boolean canAccessFile(String filename, String username) {
        Long currentUserId = parseUserId(username);
        if (currentUserId == null)
            return false;

        // Use the efficient repository method
        Document doc = documentRepository.findByFileName(filename).orElse(null);
        if (doc == null)
            return false;

        // Check if the current user is the owner
        if (doc.getUserId().equals(currentUserId)) {
            return true;
        }

        // Check user's role without passing token manually
        String role = userInfoClient.getUserRole(currentUserId);
        if ("ADMIN".equals(role)) {
            return true;
        }

        if ("MANAGER".equals(role)) {
            List<Long> managedDepts = userInfoClient.getDepartmentsManaged(currentUserId);
            UserResponseDTO owner = userInfoClient.getUserById(doc.getUserId());

            if (owner != null && owner.getDepartmentId() != null && managedDepts != null) {
                return managedDepts.contains(owner.getDepartmentId());
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
}
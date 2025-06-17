package com.daking.leave.service.impl;

import com.daking.leave.dto.response.DocumentResponse;
import com.daking.leave.model.Document;
import com.daking.leave.repository.DocumentRepository;
import com.daking.leave.service.interfaces.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.daking.leave.repository.LeaveRepository;
import com.daking.leave.client.UserInfoClient;
import com.daking.auth.api.model.User;
import com.daking.leave.model.Leave;

@Service
public class DocumentServiceImpl implements DocumentService {
    private final DocumentRepository documentRepository;
    private final LeaveRepository leaveRepository;
    private final UserInfoClient userInfoClient;
    private static final Logger logger = LoggerFactory.getLogger(DocumentServiceImpl.class);

    @Value("${app.document.upload.dir:./uploads/documents}")
    private String uploadDir;

    @Value("${app.system.token:}")
    private String systemToken;

    @Autowired
    public DocumentServiceImpl(DocumentRepository documentRepository, LeaveRepository leaveRepository,
            UserInfoClient userInfoClient) {
        this.documentRepository = documentRepository;
        this.leaveRepository = leaveRepository;
        this.userInfoClient = userInfoClient;
    }

    @Override
    @Transactional
    public DocumentResponse uploadDocument(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            logger.error("File upload failed: file is null or empty");
            throw new IllegalArgumentException("File must not be empty");
        }
        // Generate a unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : "";
        String uniqueFilename = UUID.randomUUID() + extension;
        // Store file in local storage
        File dir = new File(uploadDir);
        logger.info("Resolved upload directory: {}", dir.getAbsolutePath());
        logger.info("Original filename: {} | Unique filename: {}", originalFilename, uniqueFilename);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            logger.info("Created upload directory: {} -> {}", dir.getAbsolutePath(), created);
        }
        File dest = new File(dir, uniqueFilename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            logger.error("Failed to store file: {} | Exception: {}", dest.getAbsolutePath(), e.getMessage(), e);
            throw new RuntimeException("Failed to store file", e);
        }
        Document doc = new Document();
        doc.setUserId(userId);
        doc.setFileName(uniqueFilename);
        doc.setFileType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setUrl("/files/" + uniqueFilename);
        documentRepository.save(doc);
        return toResponse(doc);
    }

    @Override
    public DocumentResponse getDocumentById(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        return toResponse(doc);
    }

    @Override
    public List<DocumentResponse> getDocumentsByUser(Long userId) {
        return documentRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<DocumentResponse> getDocumentsByUserId(Long userId) {
        return getDocumentsByUser(userId);
    }

    @Override
    @Transactional
    public DocumentResponse updateDocumentStatus(Long id, String status) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        doc.setStatus(status);
        documentRepository.save(doc);
        return toResponse(doc);
    }

    private DocumentResponse toResponse(Document doc) {
        DocumentResponse dto = new DocumentResponse();
        dto.setId(doc.getId());
        dto.setUserId(doc.getUserId());
        dto.setFileName(doc.getFileName());
        dto.setFileType(doc.getFileType());
        dto.setFileSize(doc.getFileSize());
        dto.setUrl(doc.getUrl());
        dto.setCreatedAt(doc.getCreatedAt());
        dto.setStatus(doc.getStatus());
        // Enrich with employee name
        try {
            User user = userInfoClient.getUserById(doc.getUserId(), systemToken);
            if (user != null) {
                dto.setEmployeeName(user.getFullName() != null ? user.getFullName() : user.getEmail());
            }
        } catch (Exception e) {
            dto.setEmployeeName("");
        }
        // Enrich with leave type name
        try {
            java.util.List<Leave> leaves = leaveRepository.findByDocumentIdWithType(String.valueOf(doc.getId()));
            if (leaves != null && !leaves.isEmpty() && leaves.get(0).getLeaveType() != null) {
                dto.setLeaveTypeName(leaves.get(0).getLeaveType().getName());
            } else {
                dto.setLeaveTypeName("");
            }
        } catch (Exception e) {
            dto.setLeaveTypeName("");
        }
        return dto;
    }

    public DocumentRepository getDocumentRepository() {
        return documentRepository;
    }
}
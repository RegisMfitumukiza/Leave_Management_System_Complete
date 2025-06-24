package com.daking.leave.service.interfaces;

import com.daking.leave.dto.response.DocumentResponse;
import com.daking.leave.model.Document;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface DocumentService {
    DocumentResponse uploadDocument(Long userId, MultipartFile file);

    DocumentResponse getDocumentById(Long id);

    List<DocumentResponse> getDocumentsByUser(Long userId);

    DocumentResponse updateDocumentStatus(Long id, String status);

    List<DocumentResponse> getDocumentsByUserId(Long userId);

    List<Document> getDocumentsByIds(List<Long> ids);

    DocumentResponse toResponse(Document document);

    Document getDocumentByFileName(String filename);
}
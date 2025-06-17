package com.daking.leave.repository;

import com.daking.leave.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserId(Long userId);

    Document findByFileName(String fileName);
}
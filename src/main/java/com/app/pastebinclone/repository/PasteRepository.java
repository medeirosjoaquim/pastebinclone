package com.app.pastebinclone.repository;

import com.app.pastebinclone.models.Exposure;
import com.app.pastebinclone.models.Paste;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasteRepository extends JpaRepository<Paste, Long> {
    Optional<Paste> findByUrl(String url);

    boolean existsByUrl(String url);

    @Query("SELECT p FROM Paste p WHERE " +
            "(p.expirationDate IS NULL OR p.expirationDate > :currentDateTime) " +
            "AND p.exposure = :exposure " +
            "AND (p.password IS NULL OR p.password = '')")
    Page<Paste> findVisiblePastes(LocalDateTime currentDateTime, Exposure exposure, Pageable pageable);

    @Modifying
    @Query("UPDATE Paste p SET p.views = p.views + 1 WHERE p.id = :id")
    void incrementViews(Long id);
}

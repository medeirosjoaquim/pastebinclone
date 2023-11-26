package com.app.pastebinclone.repository;

import com.app.pastebinclone.models.Exposure;
import com.app.pastebinclone.models.Paste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasteRepository extends JpaRepository<Paste, Long> {
    Optional<Paste> findByUrl(String url);

    @Query("SELECT p FROM Paste p WHERE p.expirationDate > :currentDateTime AND p.exposure = :exposure")
    List<Paste> findNotExpiredAndPublicPastes(LocalDateTime currentDateTime, Exposure exposure);
}

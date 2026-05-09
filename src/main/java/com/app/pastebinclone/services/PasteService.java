package com.app.pastebinclone.services;

import com.app.pastebinclone.DTOs.CreatePasteDTO;
import com.app.pastebinclone.DTOs.PasteDTO;
import com.app.pastebinclone.DTOs.UpdatePasteDTO;
import com.app.pastebinclone.models.Exposure;
import com.app.pastebinclone.models.Paste;
import com.app.pastebinclone.repository.PasteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PasteService {

    private static final int URL_LENGTH = 10;
    private static final int URL_COLLISION_RETRIES = 5;

    private final PasteRepository pasteRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public PasteService(PasteRepository pasteRepository, PasswordEncoder passwordEncoder) {
        this.pasteRepository = pasteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PasteDTO createPaste(CreatePasteDTO createDto) {
        Paste paste = new Paste();
        paste.setTitle(createDto.getTitle());
        paste.setContent(createDto.getContent());
        paste.setExposure(createDto.getExposure() != null ? createDto.getExposure() : Exposure.PUBLIC);
        paste.setExpirationDate(createDto.getExpirationDate());
        paste.setLanguage(createDto.getLanguage());
        paste.setBurnAfterRead(createDto.isBurnAfterRead());

        if (createDto.getPassword() != null && !createDto.getPassword().isBlank()) {
            paste.setPassword(passwordEncoder.encode(createDto.getPassword()));
        }

        Paste saved = saveWithUniqueUrl(paste);
        return convertToDTO(saved);
    }

    public Page<PasteDTO> getAllPastes(Pageable pageable) {
        return pasteRepository
                .findVisiblePastes(LocalDateTime.now(), Exposure.PUBLIC, pageable)
                .map(this::convertToDTO);
    }

    @Transactional
    public PasteDTO getPaste(String url, String providedPassword) {
        Paste paste = pasteRepository.findByUrl(url)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paste not found"));

        if (paste.getExpirationDate() != null && paste.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Paste is expired");
        }

        if (paste.getPassword() != null && !paste.getPassword().isEmpty()) {
            if (providedPassword == null || !passwordEncoder.matches(providedPassword, paste.getPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password required");
            }
        }

        PasteDTO dto = convertToDTO(paste);
        dto.setViews(paste.getViews() + 1);

        if (paste.isBurnAfterRead()) {
            pasteRepository.delete(paste);
        } else {
            pasteRepository.incrementViews(paste.getId());
        }

        return dto;
    }

    @Transactional
    public PasteDTO updatePaste(String url, UpdatePasteDTO dto) {
        Paste paste = pasteRepository.findByUrl(url)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paste not found"));

        if (paste.getPassword() == null || paste.getPassword().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Paste has no password — updates are not allowed");
        }
        if (!passwordEncoder.matches(dto.getPassword(), paste.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong password");
        }

        if (dto.getTitle() != null) paste.setTitle(dto.getTitle());
        if (dto.getContent() != null) paste.setContent(dto.getContent());
        if (dto.getExposure() != null) paste.setExposure(dto.getExposure());
        if (dto.getExpirationDate() != null) paste.setExpirationDate(dto.getExpirationDate());
        if (dto.getLanguage() != null) paste.setLanguage(dto.getLanguage());

        return convertToDTO(pasteRepository.save(paste));
    }

    @Transactional
    public void deletePaste(String url, String providedPassword) {
        Paste paste = pasteRepository.findByUrl(url)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paste not found"));

        if (paste.getPassword() == null || paste.getPassword().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Paste has no password — cannot be deleted");
        }
        if (!passwordEncoder.matches(providedPassword, paste.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong password");
        }

        pasteRepository.delete(paste);
    }

    private PasteDTO convertToDTO(Paste paste) {
        PasteDTO dto = new PasteDTO();
        dto.setId(paste.getId());
        dto.setTitle(paste.getTitle());
        dto.setContent(paste.getContent());
        dto.setExposure(paste.getExposure());
        dto.setCreatedAt(paste.getCreatedAt());
        dto.setUpdatedAt(paste.getUpdatedAt());
        dto.setExpirationDate(paste.getExpirationDate());
        dto.setUrl(paste.getUrl());
        dto.setLanguage(paste.getLanguage());
        dto.setViews(paste.getViews());
        dto.setBurnAfterRead(paste.isBurnAfterRead());
        return dto;
    }

    private Paste saveWithUniqueUrl(Paste paste) {
        for (int attempt = 0; attempt < URL_COLLISION_RETRIES; attempt++) {
            paste.setUrl(generateShortUrl(paste));
            try {
                return pasteRepository.saveAndFlush(paste);
            } catch (DataIntegrityViolationException ignored) {
                // url collision; regenerate and retry
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate a unique URL");
    }

    private String generateShortUrl(Paste paste) {
        byte[] salt = new byte[8];
        random.nextBytes(salt);
        String input = paste.getTitle() + paste.getContent() + LocalDateTime.now() + new String(salt);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, URL_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate URL", e);
        }
    }
}

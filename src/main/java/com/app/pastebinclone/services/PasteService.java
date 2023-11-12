package com.app.pastebinclone.services;
import com.app.pastebinclone.DTOs.CreatePasteDTO;
import com.app.pastebinclone.DTOs.PasteDTO;
import com.app.pastebinclone.models.Exposure;
import com.app.pastebinclone.models.Paste;
import com.app.pastebinclone.repository.PasteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PasteService {

    private final PasteRepository pasteRepository;

    @Autowired
    public PasteService(PasteRepository pasteRepository) {
        this.pasteRepository = pasteRepository;
    }

    public Optional<PasteDTO> getPasteById(Long id) {
        Optional<Paste> paste = pasteRepository.findById(id);
        return paste.map(this::convertToDTO);
    }

    public Optional<PasteDTO> getPasteByUrl(String url) {
        Optional<Paste> paste = pasteRepository.findByUrl(url);
        return paste.map(this::convertToDTO);
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
        return dto;
    }

    public PasteDTO createPaste(CreatePasteDTO createDto) {

        Paste paste = new Paste();
        paste.setUpdatedAt(LocalDateTime.now());
        paste.setCreatedAt(LocalDateTime.now());
        paste.setExposure(createDto.getExposure() != null ? createDto.getExposure() : Exposure.PUBLIC);
        paste.setTitle(createDto.getTitle());
        paste.setPassword(generateHashPassword(paste));
        paste.setContent(createDto.getContent());
        paste.setExposure(createDto.getExposure());
        paste.setExpirationDate(createDto.getExpirationDate());
        paste.setUrl(generateShortUrl(paste));


        Paste savedPaste = pasteRepository.save(paste);
        return convertToDTO(savedPaste);
    }

    public List<PasteDTO> getAllPastes() {
        LocalDateTime now = LocalDateTime.now();
        List<Paste> pastes = pasteRepository.findAll()
                .stream()
                .filter(paste -> paste.getExposure() == Exposure.PUBLIC)
                .filter(paste -> paste.getExpirationDate() == null || paste.getExpirationDate().isAfter(now))
                .toList();

        return pastes.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public PasteDTO getPaste(String url) {
        LocalDateTime now = LocalDateTime.now();
        Optional<Paste> optionalPaste = pasteRepository.findByUrl(url);

        if (!optionalPaste.isPresent()) {
            throw new RuntimeException("Paste not found");
        }

        Paste paste = optionalPaste.get();
        if (paste.getExpirationDate() != null && paste.getExpirationDate().isBefore(now)) {
            throw new RuntimeException("Paste is expired");
        }

        return this.convertToDTO(paste);
    }

    public PasteDTO deletePaste(String url) {
        LocalDateTime now = LocalDateTime.now();
        Optional<Paste> optionalPaste = pasteRepository.findByUrl(url);

        if (!optionalPaste.isPresent()) {
            throw new RuntimeException("Paste not found");
        }

        Paste paste = optionalPaste.get();
        if (paste.getExpirationDate() != null && paste.getExpirationDate().isBefore(now)) {
            throw new RuntimeException("Paste is expired");
        }

        pasteRepository.delete(paste);
        return this.convertToDTO(paste);
    }

    private String generateShortUrl(Paste paste) {
        String originalString = paste.getTitle() + paste.getContent() + LocalDateTime.now();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(originalString.getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 10);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate URL", e);
        }
    }

    private String generateHashPassword(Paste paste) {
        String originalString = paste.getPassword() + paste.getCreatedAt();


        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(originalString.getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 10);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed", e);
        }
    }
}
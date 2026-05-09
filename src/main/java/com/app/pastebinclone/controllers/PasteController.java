package com.app.pastebinclone.controllers;

import com.app.pastebinclone.DTOs.CreatePasteDTO;
import com.app.pastebinclone.DTOs.DeletePasteDTO;
import com.app.pastebinclone.DTOs.PasteDTO;
import com.app.pastebinclone.DTOs.UpdatePasteDTO;
import com.app.pastebinclone.services.PasteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/pastes")
public class PasteController {

    private static final String PASSWORD_HEADER = "X-Paste-Password";

    private final PasteService pasteService;

    @Autowired
    public PasteController(PasteService pasteService) {
        this.pasteService = pasteService;
    }

    @PostMapping
    public ResponseEntity<PasteDTO> createPaste(@Valid @RequestBody CreatePasteDTO createPasteDTO) {
        return ResponseEntity.ok(pasteService.createPaste(createPasteDTO));
    }

    @GetMapping
    public ResponseEntity<Page<PasteDTO>> getAllPastes(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(pasteService.getAllPastes(pageable));
    }

    @GetMapping("/{url}")
    public ResponseEntity<PasteDTO> getPasteByUrl(
            @PathVariable String url,
            @RequestHeader(value = PASSWORD_HEADER, required = false) String password) {
        return ResponseEntity.ok(pasteService.getPaste(url, password));
    }

    @GetMapping(value = "/{url}/raw", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getPasteRaw(
            @PathVariable String url,
            @RequestHeader(value = PASSWORD_HEADER, required = false) String password) {
        PasteDTO dto = pasteService.getPaste(url, password);
        return ResponseEntity.ok(dto.getContent());
    }

    @PutMapping("/{url}")
    public ResponseEntity<PasteDTO> updatePaste(
            @PathVariable String url,
            @Valid @RequestBody UpdatePasteDTO updatePasteDTO) {
        return ResponseEntity.ok(pasteService.updatePaste(url, updatePasteDTO));
    }

    @DeleteMapping
    public ResponseEntity<Void> deletePaste(@Valid @RequestBody DeletePasteDTO deletePasteDTO) {
        pasteService.deletePaste(deletePasteDTO.getUrl(), deletePasteDTO.getPassword());
        return ResponseEntity.noContent().build();
    }
}

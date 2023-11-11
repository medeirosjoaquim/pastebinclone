package com.app.pastebinclone.controllers;

import com.app.pastebinclone.DTOs.CreatePasteDTO;
import com.app.pastebinclone.DTOs.PasteDTO;
import com.app.pastebinclone.services.PasteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/pastes")
public class PasteController {

    private final PasteService pasteService;

    @Autowired
    public PasteController(PasteService pasteService) {
        this.pasteService = pasteService;
    }

    @PostMapping
    public ResponseEntity<PasteDTO> createPaste(@RequestBody CreatePasteDTO createPasteDTO) {
        PasteDTO newPaste = pasteService.createPaste(createPasteDTO);
        return ResponseEntity.ok(newPaste);
    }

    @GetMapping
    public ResponseEntity<List<PasteDTO>> getAllPastes() {
        List<PasteDTO> pastes = pasteService.getAllPastes();
        return ResponseEntity.ok(pastes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PasteDTO> getPasteById(@PathVariable Long id) {
        Optional<PasteDTO> pasteDto = pasteService.getPasteById(id);
        return pasteDto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
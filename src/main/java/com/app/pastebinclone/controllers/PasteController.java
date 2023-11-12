package com.app.pastebinclone.controllers;

import com.app.pastebinclone.DTOs.CreatePasteDTO;
import com.app.pastebinclone.DTOs.PasteDTO;
import com.app.pastebinclone.models.ErrorResponse;
import com.app.pastebinclone.services.PasteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/pastes")
public class PasteController {

    private final PasteService pasteService;

    @Autowired
    public PasteController(PasteService pasteService) {
        this.pasteService = pasteService;
    }


    @ExceptionHandler(ResponseStatusException.class)

    @PostMapping
    public ResponseEntity<Object> createPaste(@RequestBody CreatePasteDTO createPasteDTO) {
        try {
            PasteDTO newPaste = pasteService.createPaste(createPasteDTO);
            return ResponseEntity.ok(newPaste);
        } catch (Exception ex) {

            ErrorResponse errorResponse = new ErrorResponse(
                    LocalDateTime.now(),
                    ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @GetMapping
    public ResponseEntity<List<PasteDTO>> getAllPastes() {
        List<PasteDTO> pastes = pasteService.getAllPastes();
        return ResponseEntity.ok(pastes);
    }

    @GetMapping("/{url}")
    public ResponseEntity<PasteDTO> getPasteById(@PathVariable String url) {
        Optional<PasteDTO> pasteDto = pasteService.getPasteByUrl(url);
        return pasteDto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
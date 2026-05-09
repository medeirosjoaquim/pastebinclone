package com.app.pastebinclone.controllers;

import com.app.pastebinclone.DTOs.PasteDTO;
import com.app.pastebinclone.config.SecurityConfig;
import com.app.pastebinclone.services.PasteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PasteController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PasteControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PasteService service;

    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void postPaste_returnsBadRequestOnMissingTitle() throws Exception {
        String body = "{\"content\":\"hi\"}";

        mvc.perform(post("/pastes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("title")));
    }

    @Test
    void postPaste_returnsOkWithDto() throws Exception {
        PasteDTO dto = new PasteDTO();
        dto.setUrl("abc1234567");
        when(service.createPaste(any())).thenReturn(dto);

        String body = json.writeValueAsString(java.util.Map.of("title", "t", "content", "c"));

        mvc.perform(post("/pastes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("abc1234567"));
    }

    @Test
    void getList_returnsPagedResponse() throws Exception {
        Page<PasteDTO> page = new PageImpl<>(List.of(new PasteDTO()));
        when(service.getAllPastes(any())).thenReturn(page);

        mvc.perform(get("/pastes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getByUrl_returns410Expired() throws Exception {
        when(service.getPaste(eq("xyz"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.GONE, "Paste is expired"));

        mvc.perform(get("/pastes/xyz"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("Paste is expired"));
    }

    @Test
    void getByUrl_returns401WhenPasswordRequired() throws Exception {
        when(service.getPaste(eq("xyz"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password required"));

        mvc.perform(get("/pastes/xyz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getByUrl_passesPasswordHeaderToService() throws Exception {
        when(service.getPaste(eq("xyz"), eq("secret"))).thenReturn(new PasteDTO());

        mvc.perform(get("/pastes/xyz").header("X-Paste-Password", "secret"))
                .andExpect(status().isOk());
    }

    @Test
    void getRaw_returnsTextPlainContent() throws Exception {
        PasteDTO dto = new PasteDTO();
        dto.setContent("raw-body");
        when(service.getPaste(eq("xyz"), any())).thenReturn(dto);

        mvc.perform(get("/pastes/xyz/raw"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("raw-body"));
    }

    @Test
    void delete_returns204OnSuccess() throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("url", "xyz", "password", "pw"));

        mvc.perform(delete("/pastes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns400OnMissingFields() throws Exception {
        mvc.perform(delete("/pastes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}

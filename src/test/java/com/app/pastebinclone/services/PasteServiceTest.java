package com.app.pastebinclone.services;

import com.app.pastebinclone.DTOs.CreatePasteDTO;
import com.app.pastebinclone.DTOs.PasteDTO;
import com.app.pastebinclone.DTOs.UpdatePasteDTO;
import com.app.pastebinclone.models.Exposure;
import com.app.pastebinclone.models.Paste;
import com.app.pastebinclone.repository.PasteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasteServiceTest {

    @Mock
    PasteRepository repository;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    PasteService service;

    @BeforeEach
    void setup() {
        service = new PasteService(repository, passwordEncoder);
    }

    @Test
    void createPaste_defaultsExposureToPublic_whenNull() {
        when(repository.saveAndFlush(any(Paste.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatePasteDTO dto = new CreatePasteDTO();
        dto.setTitle("hi");
        dto.setContent("body");
        dto.setExposure(null);

        PasteDTO result = service.createPaste(dto);

        assertThat(result.getExposure()).isEqualTo(Exposure.PUBLIC);
    }

    @Test
    void createPaste_withoutPassword_storesNullPassword() {
        ArgumentCaptor<Paste> captor = ArgumentCaptor.forClass(Paste.class);
        when(repository.saveAndFlush(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        CreatePasteDTO dto = new CreatePasteDTO();
        dto.setTitle("t");
        dto.setContent("c");

        service.createPaste(dto);

        assertThat(captor.getValue().getPassword()).isNull();
    }

    @Test
    void createPaste_hashesPassword_whenProvided() {
        ArgumentCaptor<Paste> captor = ArgumentCaptor.forClass(Paste.class);
        when(repository.saveAndFlush(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        CreatePasteDTO dto = new CreatePasteDTO();
        dto.setTitle("t");
        dto.setContent("c");
        dto.setPassword("secret");

        service.createPaste(dto);

        String stored = captor.getValue().getPassword();
        assertThat(stored).isNotEqualTo("secret");
        assertThat(passwordEncoder.matches("secret", stored)).isTrue();
    }

    @Test
    void createPaste_retriesOnUrlCollision() {
        when(repository.saveAndFlush(any(Paste.class)))
                .thenThrow(new DataIntegrityViolationException("dup"))
                .thenAnswer(inv -> inv.getArgument(0));

        CreatePasteDTO dto = new CreatePasteDTO();
        dto.setTitle("t");
        dto.setContent("c");

        PasteDTO result = service.createPaste(dto);

        assertThat(result.getUrl()).isNotBlank();
        verify(repository, times(2)).saveAndFlush(any(Paste.class));
    }

    @Test
    void getPaste_returnsGoneOnExpired() {
        Paste expired = new Paste();
        expired.setUrl("e");
        expired.setExpirationDate(LocalDateTime.now().minusMinutes(1));
        when(repository.findByUrl("e")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.getPaste("e", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void getPaste_returns401WhenPasswordRequiredAndMissing() {
        Paste pwd = new Paste();
        pwd.setUrl("p");
        pwd.setPassword(passwordEncoder.encode("right"));
        when(repository.findByUrl("p")).thenReturn(Optional.of(pwd));

        assertThatThrownBy(() -> service.getPaste("p", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Password");
    }

    @Test
    void getPaste_returns401OnWrongPassword() {
        Paste pwd = new Paste();
        pwd.setUrl("p");
        pwd.setPassword(passwordEncoder.encode("right"));
        when(repository.findByUrl("p")).thenReturn(Optional.of(pwd));

        assertThatThrownBy(() -> service.getPaste("p", "wrong"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getPaste_succeedsWithCorrectPassword_andIncrementsViews() {
        Paste pwd = new Paste();
        pwd.setId(1L);
        pwd.setUrl("p");
        pwd.setPassword(passwordEncoder.encode("right"));
        pwd.setContent("body");
        when(repository.findByUrl("p")).thenReturn(Optional.of(pwd));

        PasteDTO result = service.getPaste("p", "right");

        assertThat(result.getContent()).isEqualTo("body");
        verify(repository).incrementViews(1L);
        verify(repository, never()).delete(any());
    }

    @Test
    void getPaste_burnAfterRead_deletesAfterReturning() {
        Paste burn = new Paste();
        burn.setId(2L);
        burn.setUrl("b");
        burn.setBurnAfterRead(true);
        burn.setContent("once");
        when(repository.findByUrl("b")).thenReturn(Optional.of(burn));

        PasteDTO result = service.getPaste("b", null);

        assertThat(result.getContent()).isEqualTo("once");
        verify(repository).delete(burn);
        verify(repository, never()).incrementViews(any());
    }

    @Test
    void deletePaste_rejectsWhenPasteHasNoPassword() {
        Paste p = new Paste();
        p.setUrl("u");
        when(repository.findByUrl("u")).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.deletePaste("u", "anything"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no password");
    }

    @Test
    void updatePaste_appliesPartialChanges() {
        Paste p = new Paste();
        p.setId(3L);
        p.setUrl("u");
        p.setTitle("old-title");
        p.setContent("old-content");
        p.setPassword(passwordEncoder.encode("pw"));
        when(repository.findByUrl("u")).thenReturn(Optional.of(p));
        when(repository.save(any(Paste.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePasteDTO dto = new UpdatePasteDTO();
        dto.setPassword("pw");
        dto.setTitle("new-title");

        PasteDTO result = service.updatePaste("u", dto);

        assertThat(result.getTitle()).isEqualTo("new-title");
        assertThat(result.getContent()).isEqualTo("old-content");
    }
}

package com.app.pastebinclone;

import com.app.pastebinclone.DTOs.CreatePasteDTO;
import com.app.pastebinclone.DTOs.DeletePasteDTO;
import com.app.pastebinclone.DTOs.PasteDTO;
import com.app.pastebinclone.models.Exposure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PasteEndToEndTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TestRestTemplate http;

    @Test
    void create_view_increment_delete_flow() {
        CreatePasteDTO create = new CreatePasteDTO();
        create.setTitle("hello");
        create.setContent("world");
        create.setExposure(Exposure.PUBLIC);
        create.setPassword("pw");

        ResponseEntity<PasteDTO> created = http.postForEntity("/pastes", create, PasteDTO.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        String url = created.getBody().getUrl();
        assertThat(url).hasSize(10);

        ResponseEntity<PasteDTO> notAuthed = http.getForEntity("/pastes/" + url, PasteDTO.class);
        assertThat(notAuthed.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Paste-Password", "pw");
        ResponseEntity<PasteDTO> firstView = http.exchange("/pastes/" + url, HttpMethod.GET,
                new HttpEntity<>(headers), PasteDTO.class);
        assertThat(firstView.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstView.getBody().getContent()).isEqualTo("world");
        assertThat(firstView.getBody().getViews()).isEqualTo(1);

        ResponseEntity<PasteDTO> secondView = http.exchange("/pastes/" + url, HttpMethod.GET,
                new HttpEntity<>(headers), PasteDTO.class);
        assertThat(secondView.getBody().getViews()).isEqualTo(2);

        DeletePasteDTO delete = new DeletePasteDTO();
        delete.setUrl(url);
        delete.setPassword("pw");
        ResponseEntity<Void> del = http.exchange("/pastes", HttpMethod.DELETE,
                new HttpEntity<>(delete), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<PasteDTO> gone = http.exchange("/pastes/" + url, HttpMethod.GET,
                new HttpEntity<>(headers), PasteDTO.class);
        assertThat(gone.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void burnAfterRead_deletesAfterFirstFetch() {
        CreatePasteDTO create = new CreatePasteDTO();
        create.setTitle("burn");
        create.setContent("vanishes");
        create.setBurnAfterRead(true);

        String url = http.postForEntity("/pastes", create, PasteDTO.class).getBody().getUrl();

        ResponseEntity<PasteDTO> first = http.getForEntity("/pastes/" + url, PasteDTO.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody().getContent()).isEqualTo("vanishes");

        ResponseEntity<PasteDTO> second = http.getForEntity("/pastes/" + url, PasteDTO.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void list_excludesPasswordProtectedAndExpired() {
        CreatePasteDTO open = new CreatePasteDTO();
        open.setTitle("open");
        open.setContent("c");
        http.postForEntity("/pastes", open, PasteDTO.class);

        CreatePasteDTO secret = new CreatePasteDTO();
        secret.setTitle("secret");
        secret.setContent("c");
        secret.setPassword("pw");
        http.postForEntity("/pastes", secret, PasteDTO.class);

        ResponseEntity<Map> page = http.getForEntity("/pastes", Map.class);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        Object content = page.getBody().get("content");
        assertThat(content.toString()).contains("open").doesNotContain("\"title\":\"secret\"");
    }

    @Test
    void rawEndpoint_returnsPlainTextContent() {
        CreatePasteDTO create = new CreatePasteDTO();
        create.setTitle("raw");
        create.setContent("just-the-body");
        String url = http.postForEntity("/pastes", create, PasteDTO.class).getBody().getUrl();

        ResponseEntity<String> raw = http.getForEntity("/pastes/" + url + "/raw", String.class);
        assertThat(raw.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(raw.getBody()).isEqualTo("just-the-body");
    }

    @Test
    void expiredPaste_returnsGone() {
        CreatePasteDTO create = new CreatePasteDTO();
        create.setTitle("exp");
        create.setContent("c");
        create.setExpirationDate(LocalDateTime.now().plusSeconds(2));
        String url = http.postForEntity("/pastes", create, PasteDTO.class).getBody().getUrl();

        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ResponseEntity<PasteDTO> gone = http.getForEntity("/pastes/" + url, PasteDTO.class);
        assertThat(gone.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }
}

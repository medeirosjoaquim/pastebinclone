package com.app.pastebinclone.repository;

import com.app.pastebinclone.models.Exposure;
import com.app.pastebinclone.models.Paste;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PasteRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    PasteRepository repository;

    @Autowired
    EntityManager entityManager;

    private Paste newPaste(String url, Exposure exposure, LocalDateTime expiration, String password) {
        Paste p = new Paste();
        p.setUrl(url);
        p.setTitle("t");
        p.setContent("c");
        p.setExposure(exposure);
        p.setExpirationDate(expiration);
        p.setPassword(password);
        return p;
    }

    @Test
    void findVisiblePastes_includesNullExpiration_andExcludesPasswordProtected() {
        repository.save(newPaste("a", Exposure.PUBLIC, null, null));
        repository.save(newPaste("b", Exposure.PUBLIC, LocalDateTime.now().plusDays(1), null));
        repository.save(newPaste("c", Exposure.PUBLIC, LocalDateTime.now().minusDays(1), null));
        repository.save(newPaste("d", Exposure.PRIVATE, null, null));
        repository.save(newPaste("e", Exposure.PUBLIC, null, "{bcrypt}hashed"));

        Page<Paste> page = repository.findVisiblePastes(LocalDateTime.now(), Exposure.PUBLIC, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Paste::getUrl).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void incrementViews_isAtomic() {
        Paste saved = repository.save(newPaste("inc", Exposure.PUBLIC, null, null));
        repository.flush();
        repository.incrementViews(saved.getId());
        repository.incrementViews(saved.getId());
        entityManager.flush();
        entityManager.clear();

        Paste reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getViews()).isEqualTo(2);
    }

    @Test
    void uniqueUrl_constraintEnforced() {
        repository.saveAndFlush(newPaste("dup", Exposure.PUBLIC, null, null));
        assertThatThrownBy(() ->
                repository.saveAndFlush(newPaste("dup", Exposure.PUBLIC, null, null))
        ).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void existsByUrl_works() {
        repository.save(newPaste("ex", Exposure.PUBLIC, null, null));
        assertThat(repository.existsByUrl("ex")).isTrue();
        assertThat(repository.existsByUrl("nope")).isFalse();
    }
}

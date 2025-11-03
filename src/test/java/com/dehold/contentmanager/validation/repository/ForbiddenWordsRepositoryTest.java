package com.dehold.contentmanager.validation.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ForbiddenWordsRepositoryTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @InjectMocks
    ForbiddenWordsRepository cut;

    @Test
    void whenQueryForbiddenWordsByUserAndContentType_thenCallJdbcTemplateWithRightArguments() {
        UUID userId = UUID.randomUUID();
        String contentType = "blogpost";

        cut.findByUserIdAndContentType(userId, contentType);

        verify(jdbcTemplate, times(1)).query(
                "SELECT * FROM forbidden_words WHERE user_id = ? AND content_type = ?",
                cut.FORBIDDEN_WORDS_ROW_MAPPER,
                userId,
                contentType
        );
    }
}
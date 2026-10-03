package com.shortener.urlshortener.service;

import com.shortener.urlshortener.model.ShortUrl;
import com.shortener.urlshortener.repository.ShortUrlRepository;
import com.shortener.urlshortener.service.ShortUrlService.ShortUrlExpiredException;
import com.shortener.urlshortener.service.ShortUrlService.ShortUrlNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private UrlValidator validator;

    @InjectMocks
    private ShortUrlService service;

    private ShortUrl testUrl;

    @BeforeEach
    void setUp() {
        testUrl = ShortUrl.builder()
                .id(1L)
                .code("ABC123")
                .originalUrl("https://example.com")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .clickCount(0L)
                .build();
    }

    /** Any URL is valid, and save() returns exactly what the service built. */
    private void allowAnyUrlAndEchoSave() {
        when(validator.isValid(anyString())).thenReturn(true);
        when(repository.save(any(ShortUrl.class))).thenAnswer(returnsFirstArg());
    }

    // ---------- create ----------

    @Test
    void testCreateShortUrl() {
        allowAnyUrlAndEchoSave();

        ShortUrl result = service.createShortUrl("https://example.com", 30);

        assertNotNull(result.getCode());
        assertEquals(8, result.getCode().length());
        assertEquals("https://example.com", result.getOriginalUrl());
        assertEquals(0L, result.getClickCount());
        assertNotNull(result.getCreatedAt());
        verify(repository, times(1)).save(any(ShortUrl.class));
    }

    @Test
    void testCreateShortUrlInvalidUrl() {
        when(validator.isValid("invalid-url")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.createShortUrl("invalid-url", 30));
        verify(repository, never()).save(any(ShortUrl.class));
    }

    @Test
    void testExpirationDateSet() {
        allowAnyUrlAndEchoSave();

        ShortUrl result = service.createShortUrl("https://example.com", 30);

        long days = Duration.between(result.getCreatedAt(), result.getExpiresAt()).toDays();
        assertEquals(30, days);
    }

    @Test
    void testZeroExpirationUsesDefault() {
        allowAnyUrlAndEchoSave();

        ShortUrl result = service.createShortUrl("https://example.com", 0);

        long days = Duration.between(result.getCreatedAt(), result.getExpiresAt()).toDays();
        assertEquals(ShortUrlService.DEFAULT_EXPIRATION_DAYS, days);
    }

    @Test
    void testGenerateUniqueCode() {
        allowAnyUrlAndEchoSave();

        ShortUrl url1 = service.createShortUrl("https://example1.com", 30);
        ShortUrl url2 = service.createShortUrl("https://example2.com", 30);

        assertNotNull(url1.getCode());
        assertNotNull(url2.getCode());
        assertNotEquals(url1.getCode(), url2.getCode());
    }

    @Test
    void testCodeCollisionIsRetried() {
        allowAnyUrlAndEchoSave();
        when(repository.findByCode(anyString()))
                .thenReturn(Optional.of(testUrl))   // first code already taken
                .thenReturn(Optional.empty());      // second code is free

        ShortUrl result = service.createShortUrl("https://example.com", 30);

        assertNotNull(result.getCode());
        verify(repository, times(2)).findByCode(anyString());
    }

    @Test
    void testCodeGenerationGivesUpAfterMaxAttempts() {
        when(validator.isValid(anyString())).thenReturn(true);
        when(repository.findByCode(anyString())).thenReturn(Optional.of(testUrl));

        assertThrows(IllegalStateException.class,
                () -> service.createShortUrl("https://example.com", 30));
        verify(repository, never()).save(any(ShortUrl.class));
    }

    // ---------- lookup ----------

    @Test
    void testGetShortUrl() {
        when(repository.findByCode("ABC123")).thenReturn(Optional.of(testUrl));

        ShortUrl result = service.getShortUrl("ABC123");

        assertEquals("ABC123", result.getCode());
        assertEquals("https://example.com", result.getOriginalUrl());
    }

    @Test
    void testGetShortUrlNotFound() {
        when(repository.findByCode("NOTFOUND")).thenReturn(Optional.empty());

        assertThrows(ShortUrlNotFoundException.class, () -> service.getShortUrl("NOTFOUND"));
    }

    // ---------- clicks ----------

    @Test
    void testRecordClick() {
        when(repository.incrementClickCount("ABC123")).thenReturn(1);

        service.recordClick("ABC123");

        verify(repository, times(1)).incrementClickCount("ABC123");
    }

    @Test
    void testRecordClickUnknownCode() {
        when(repository.incrementClickCount("NOTFOUND")).thenReturn(0);

        assertThrows(ShortUrlNotFoundException.class, () -> service.recordClick("NOTFOUND"));
    }

    @Test
    void testRecordClickIgnoresBlankCode() {
        service.recordClick("   ");

        verifyNoInteractions(repository);
    }

    @Test
    void testResolveAndRecordClick() {
        when(repository.findByCode("ABC123")).thenReturn(Optional.of(testUrl));
        when(repository.incrementClickCount("ABC123")).thenReturn(1);

        ShortUrl result = service.resolveAndRecordClick("ABC123");

        assertEquals("https://example.com", result.getOriginalUrl());
        verify(repository, times(1)).incrementClickCount("ABC123");
        verify(repository, times(2)).findByCode("ABC123"); // before and after the update
    }

    @Test
    void testResolveExpiredUrlThrowsAndDoesNotCount() {
        testUrl.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(repository.findByCode("ABC123")).thenReturn(Optional.of(testUrl));

        assertThrows(ShortUrlExpiredException.class, () -> service.resolveAndRecordClick("ABC123"));
        verify(repository, never()).incrementClickCount(anyString());
    }
}
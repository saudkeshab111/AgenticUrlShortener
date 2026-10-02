package com.shortener.urlshortener;

import com.shortener.urlshortener.model.ShortUrl;
import com.shortener.urlshortener.repository.ShortUrlRepository;
import com.shortener.urlshortener.service.ShortUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository repository;

    @InjectMocks
    private ShortUrlService service;

    @BeforeEach
    void setUp() {
        // Mocks are injected by Mockito
    }

    @Test
    void testCreateValidUrl() {
        when(repository.existsByCode(any())).thenReturn(false);
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl created = service.create("https://www.example.com", 0);

        assertNotNull(created.getCode());
        assertEquals("https://www.example.com", created.getOriginalUrl());
        assertEquals(0L, created.getClickCount());
    }

    @Test
    void testCreateWithExpiration() {
        when(repository.existsByCode(any())).thenReturn(false);
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl created = service.create("https://www.example.com", 30);

        assertNotNull(created.getExpiresAt());
    }

    @Test
    void testCreateInvalidUrlThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.create("invalid", 0));
    }

    @Test
    void testGetByCode() {
        ShortUrl url = new ShortUrl();
        url.setCode("ABC123");
        url.setOriginalUrl("https://www.example.com");
        url.setCreatedAt(Instant.now());
        url.setClickCount(0L);

        when(repository.findByCode("ABC123")).thenReturn(Optional.of(url));
        when(repository.save(any(ShortUrl.class))).thenReturn(url);

        Optional<ShortUrl> found = service.getByCode("ABC123");

        assertTrue(found.isPresent());
        assertEquals("ABC123", found.get().getCode());
    }
}
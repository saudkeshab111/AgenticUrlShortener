package com.shortener.urlshortener.repository;

import com.shortener.urlshortener.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ShortUrl persistence.
 */
@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    /**
     * Find a shortened URL by its code.
     */
    Optional<ShortUrl> findByCode(String code);

    /**
     * Check if a code exists.
     */
    boolean existsByCode(String code);
}
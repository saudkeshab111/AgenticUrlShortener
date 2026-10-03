package com.shortener.urlshortener.repository;

import com.shortener.urlshortener.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Atomically increments the click count in a single UPDATE.
     * Avoids lost updates when two clicks arrive at the same time.
     * Returns the number of rows updated (0 means the code does not exist).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ShortUrl s SET s.clickCount = s.clickCount + 1 WHERE s.code = :code")
    int incrementClickCount(@Param("code") String code);
}
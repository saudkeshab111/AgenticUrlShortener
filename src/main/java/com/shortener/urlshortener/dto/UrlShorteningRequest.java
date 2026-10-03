package com.shortener.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlShorteningRequest {

    @JsonProperty("originalUrl")
    private String originalUrl;

    @JsonProperty("expirationDays")
    @Builder.Default
    private Integer expirationDays = 30;
}
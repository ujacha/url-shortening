package net.ujacha.urlshortening.service;

import lombok.RequiredArgsConstructor;
import net.ujacha.urlshortening.dto.ShortUrlDTO;
import net.ujacha.urlshortening.entity.ShortUrl;
import net.ujacha.urlshortening.repository.ShortUrlRepository;
import net.ujacha.urlshortening.util.Base62Support;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;

    @Value("${short-url.seq.min}")
    private long offset;


    @Transactional
    public ShortUrlDTO createShortUrl(String originalUrl) {
        // 1. check exist original url
        // 1.1 exist original url
        //      -> inc create count, save
        //      -> return find short url

        ShortUrl shortUrl = shortUrlRepository.findByOriginalUrl(originalUrl).orElse(null);
        if (shortUrl != null) {
            shortUrl.incCreateRequestCount();
            return entityToDTO(shortUrl);
        }

        // 2. build short url entity
        shortUrl = new ShortUrl(originalUrl);
        shortUrlRepository.save(shortUrl);

        // 3. generate short key
        // 4. return short url
        return entityToDTO(shortUrl);
    }

    @Transactional
    public ShortUrlDTO getShortUrl(String shortKey) {

        // 1. find by short key
        ShortUrl shortUrl = shortUrlRepository.findById(Base62Support.decode(shortKey, offset)).orElse(null);

        // 1.1 not found -> return null
        if (shortUrl == null) {
            return null;
        }

        // 2. inc redirect count
        shortUrl.incRedirectRequestCount();

        // 3. return short url dto
        return entityToDTO(shortUrl);
    }

    private ShortUrlDTO entityToDTO(ShortUrl shortUrl) {

        if (shortUrl == null) return null;

        return ShortUrlDTO.builder()
                .shortKey(Base62Support.encode(shortUrl.getSeq(), offset))
                .originalUrl(shortUrl.getOriginalUrl())
                .build();
    }
}

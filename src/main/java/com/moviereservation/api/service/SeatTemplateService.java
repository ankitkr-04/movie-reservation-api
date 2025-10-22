package com.moviereservation.api.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviereservation.api.domain.entities.SeatTemplate;
import com.moviereservation.api.repository.SeatTemplateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class SeatTemplateService {
    private final SeatTemplateRepository seatTemplateRepository;

    @Cacheable(value = "seatTemplates", key = "#screenNumber", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<SeatTemplate> getTemplatesForScreen(final Short screenNumber) {

        return seatTemplateRepository.findByScreenNumber(screenNumber);
    }

    @CachePut(value = "seatTemplates", key = "#seatTemplate.screenNumber")
    public List<SeatTemplate> updateSeatTemplate(final SeatTemplate seatTemplate) {
        seatTemplateRepository.save(seatTemplate);
        return seatTemplateRepository.findByScreenNumber(seatTemplate.getScreenNumber());
    }

    @CacheEvict(value = "seatTemplates", key = "#screenNumber")
    public void deleteTemplatesForScreen(final Short screenNumber) {
        seatTemplateRepository.deleteByScreenNumber(screenNumber);
    }

    @CacheEvict(value = "seatTemplates", allEntries = true)
    public void evictAllCacheValues() {
        log.info("Evicted all cache entries for seatTemplates");

    }

    public void preloadAllSeatTemplates() {
        for ( short i = 1; i <= 5; i++) {
            final List<SeatTemplate> templates = getTemplatesForScreen(i);
            log.info("Preloaded {} seat templates for screen number {}", templates.size(), i);
        }
    }

}

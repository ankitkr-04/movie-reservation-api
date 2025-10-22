package com.moviereservation.api.service.cache;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.moviereservation.api.service.SeatTemplateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupService {
    private final SeatTemplateService seatTemplateService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCaches() {
        log.info("Warming up caches...");

        // Preload seat templates into cache
        seatTemplateService.preloadAllSeatTemplates();

        log.info("Cache warm-up completed.");
    }
}

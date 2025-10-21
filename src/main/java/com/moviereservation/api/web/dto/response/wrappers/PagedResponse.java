package com.moviereservation.api.web.dto.response.wrappers;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import lombok.Getter;

@Getter
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    private PagedResponse() {
    }

    /**
     * Create PagedResponse from Spring Data Page object.
     * Maps entities to DTOs using provided mapper function.
     *
     * @param page   Spring Data Page object
     * @param mapper Function to convert Entity → DTO
     * @param <E>    Entity type
     * @param <D>    DTO type
     * @return PagedResponse with DTO content
     */

    public static <E, D> PagedResponse<D> of(
            final Page<E> page,
            final Function<E, D> mapper) {
        final PagedResponse<D> pagedResponse = new PagedResponse<>();

        pagedResponse.content = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());

        pagedResponse.page = page.getNumber();
        pagedResponse.size = page.getSize();
        pagedResponse.totalElements = page.getTotalElements();
        pagedResponse.totalPages = page.getTotalPages();
        pagedResponse.hasNext = page.hasNext();
        pagedResponse.hasPrevious = page.hasPrevious();
        return pagedResponse;
    }

    /**
     * Create PagedResponse when content is already DTOs.
     * Use this when you've already mapped entities to DTOs.
     *
     * @param page Spring Data Page object (already contains DTOs)
     * @param <D>  DTO type
     * @return PagedResponse
     */
    public static <D> PagedResponse<D> of(final Page<D> page) {
        return of(page, Function.identity()); // Identity function = no mapping
    }

    /**
     * Create empty PagedResponse (for when no results found).
     *
     * @param <D> DTO type
     * @return Empty PagedResponse
     */
    public static <D> PagedResponse<D> empty() {
        final PagedResponse<D> response = new PagedResponse<>();
        response.content = List.of();
        response.page = 0;
        response.size = 0;
        response.totalElements = 0;
        response.totalPages = 0;
        response.hasNext = false;
        response.hasPrevious = false;
        return response;
    }
}
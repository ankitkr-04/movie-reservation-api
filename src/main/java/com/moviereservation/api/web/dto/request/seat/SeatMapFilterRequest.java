package com.moviereservation.api.web.dto.request.seat;

import java.util.List;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class SeatMapFilterRequest {

    @Pattern(regexp = "[A-Z](,[A-Z])*", message = "Row labels must be single uppercase letters, comma-separated")
    private final String rowLabels; // e.g., "A,B,C" to filter specific rows

    // Parse rowLabels to List<Character> for easier processing
    public List<Character> getRowLabelsAsList() {
        if (rowLabels == null || rowLabels.isEmpty()) {
            return List.of();
        }
        return rowLabels.chars()
                .mapToObj(ch -> (char) ch)
                .toList();
    }
}
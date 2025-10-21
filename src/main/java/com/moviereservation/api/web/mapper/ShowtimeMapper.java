package com.moviereservation.api.web.mapper;

import org.mapstruct.*;

import com.moviereservation.api.domain.entities.Showtime;
import com.moviereservation.api.web.dto.request.showtime.CreateShowtimeRequest;
import com.moviereservation.api.web.dto.request.showtime.UpdateShowtimeRequest;
import com.moviereservation.api.web.dto.response.showtime.ShowtimeAdminResponse;
import com.moviereservation.api.web.dto.response.showtime.ShowtimeCustomerResponse;

@Mapper(componentModel = "spring")
public interface ShowtimeMapper {
    ShowtimeCustomerResponse toCustomerResponse(final Showtime showtime);

    ShowtimeAdminResponse toAdminResponse(final Showtime showtime);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
    @Mapping(target = "availableSeatsCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "endTime", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "movie", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void toEntity(CreateShowtimeRequest request, @MappingTarget Showtime showtime);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
    void toEntity(UpdateShowtimeRequest request, @MappingTarget Showtime showtime);

}

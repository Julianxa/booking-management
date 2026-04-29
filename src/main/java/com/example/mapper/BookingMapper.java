package com.example.mapper;

import com.example.model.dto.*;
import com.example.model.entity.BookingAttendees;
import com.example.model.entity.Bookings;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import java.util.List;

import static com.example.constant.Enums.BookingStatus.SUCCESS;

@Mapper(componentModel = "spring")
public interface BookingMapper {
    default CreateBookingResponseDTO toCreateResponseDto(Bookings booking, List<CreateBookingRequestDTO.BookingEventDTO> eventList, String promoCode) {
        CreateBookingResponseDTO createBookingResponseDTO = new CreateBookingResponseDTO();
        createBookingResponseDTO.setId(booking.getRefNo());
        createBookingResponseDTO.setStatus(SUCCESS);
        createBookingResponseDTO.setBookingEvents(eventList);
        createBookingResponseDTO.setTotalPaidAmount(booking.getTotalPaidPrice());
        createBookingResponseDTO.setDiscount(booking.getDiscount());
        createBookingResponseDTO.setFinalPaidAmount(booking.getFinalPaidAmount());
        createBookingResponseDTO.setPromoCode(promoCode);
        return createBookingResponseDTO;
    }

    default GetListBookingResponseDTO toGetListResponse(Page<Bookings> page, List<CreateBookingResponseDTO> createBookingResponseDTO) {

        GetListBookingResponseDTO response = new GetListBookingResponseDTO();

        response.setContent(createBookingResponseDTO);
        response.setPageable(createPageableDetail(page));
        response.setSort(createSortDetail(page.getSort()));
        response.setLast(page.isLast());
        response.setFirst(page.isFirst());
        response.setTotalPages(page.getTotalPages());
        response.setTotalElements(page.getTotalElements());
        response.setSize(page.getSize());
        response.setNumber(page.getNumber());
        response.setNumberOfElements(page.getNumberOfElements());
        response.setEmpty(page.isEmpty());

        return response;
    }

    private GetListBookingResponseDTO.PageableDetail createPageableDetail(Page<?> page) {
        GetListBookingResponseDTO.PageableDetail pd = new GetListBookingResponseDTO.PageableDetail();
        pd.setPageNumber(page.getNumber());
        pd.setPageSize(page.getSize());
        pd.setOffset(page.getPageable().getOffset());
        pd.setPaged(page.getPageable().isPaged());
        pd.setUnpaged(page.getPageable().isUnpaged());
        pd.setSort(createSortDetail(page.getSort()));
        return pd;
    }

    private GetListBookingResponseDTO.SortDetail createSortDetail(Sort sort) {
        GetListBookingResponseDTO.SortDetail sd = new GetListBookingResponseDTO.SortDetail();
        sd.setEmpty(sort.isEmpty());
        sd.setSorted(sort.isSorted());
        sd.setUnsorted(sort.isUnsorted());
        return sd;
    }

    private GetListParticipantsResponseDTO.PageableDetail createParticipantsPageableDetail(Page<?> page) {
        GetListParticipantsResponseDTO.PageableDetail pd = new GetListParticipantsResponseDTO.PageableDetail();
        pd.setPageNumber(page.getNumber());
        pd.setPageSize(page.getSize());
        pd.setOffset(page.getPageable().getOffset());
        pd.setPaged(page.getPageable().isPaged());
        pd.setUnpaged(page.getPageable().isUnpaged());
        pd.setSort(createParticipantsSortDetail(page.getSort()));
        return pd;
    }

    private GetListParticipantsResponseDTO.SortDetail createParticipantsSortDetail(Sort sort) {
        GetListParticipantsResponseDTO.SortDetail sd = new GetListParticipantsResponseDTO.SortDetail();
        sd.setEmpty(sort.isEmpty());
        sd.setSorted(sort.isSorted());
        sd.setUnsorted(sort.isUnsorted());
        return sd;
    }

    default CreateBookingRequestDTO.AttendeeDTO toAttendeeDTO(BookingAttendees attendee) {
        if (attendee == null) {
            return null;
        }

        return CreateBookingRequestDTO.AttendeeDTO.builder()
                .firstName(attendee.getFirstName())
                .lastName(attendee.getLastName())
                .email(attendee.getEmail())
                .phone(attendee.getPhone())
                .gender(attendee.getGender())
                .country(attendee.getCountry())
                .sequence(attendee.getSequence())
                .build();
    }

    default GetListParticipantsResponseDTO toGetParticipantsResponse(Page<BookingAttendees> page) {
        List<CreateBookingRequestDTO.AttendeeDTO> content = page.getContent().stream()
                .map(this::toAttendeeDTO)
                .toList();

        GetListParticipantsResponseDTO response = new GetListParticipantsResponseDTO();

        response.setContent(content);
        response.setPageable(createParticipantsPageableDetail(page));
        response.setSort(createParticipantsSortDetail(page.getSort()));
        response.setLast(page.isLast());
        response.setFirst(page.isFirst());
        response.setTotalPages(page.getTotalPages());
        response.setTotalElements(page.getTotalElements());
        response.setSize(page.getSize());
        response.setNumber(page.getNumber());
        response.setNumberOfElements(page.getNumberOfElements());
        response.setEmpty(page.isEmpty());

        return response;
    }

}

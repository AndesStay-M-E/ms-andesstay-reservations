package cl.duoc.andesstay.reservations.service;

import cl.duoc.andesstay.reservations.dto.ReservationRequest;
import cl.duoc.andesstay.reservations.dto.ReservationResponse;
import cl.duoc.andesstay.reservations.entity.Reservation;
import cl.duoc.andesstay.reservations.entity.ReservationStatus;
import cl.duoc.andesstay.reservations.exception.ResourceNotFoundException;
import cl.duoc.andesstay.reservations.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public ReservationService(
            ReservationRepository reservationRepository
    ) {
        this.reservationRepository = reservationRepository;
    }

    public ReservationResponse create(
            String userEmail,
            ReservationRequest request
    ) {
        validateDates(request);

        Reservation reservation = new Reservation();
        reservation.setUserEmail(userEmail);
        applyRequest(reservation, request);
        reservation.setStatus(ReservationStatus.PENDING);

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findAll() {
        return reservationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findByUserEmail(
            String userEmail
    ) {
        return reservationRepository
                .findByUserEmailIgnoreCaseOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ReservationResponse update(
            Long id,
            ReservationRequest request
    ) {
        validateDates(request);

        Reservation reservation = findEntityById(id);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException(
                    "No se puede modificar una reserva cancelada"
            );
        }

        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new IllegalStateException(
                    "No se puede modificar una reserva completada"
            );
        }

        applyRequest(reservation, request);

        return toResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse confirm(Long id) {
        Reservation reservation = findEntityById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                    "Solo se pueden confirmar reservas pendientes"
            );
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);

        return toResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse cancel(Long id) {
        Reservation reservation = findEntityById(id);

        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new IllegalStateException(
                    "No se puede cancelar una reserva completada"
            );
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException(
                    "La reserva ya se encuentra cancelada"
            );
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        return toResponse(reservationRepository.save(reservation));
    }

    private Reservation findEntityById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la reserva con ID " + id
                ));
    }

    private void validateDates(ReservationRequest request) {
        if (!request.checkOutDate().isAfter(request.checkInDate())) {
            throw new IllegalArgumentException(
                    "La fecha de salida debe ser posterior a la fecha de entrada"
            );
        }
    }

    private void applyRequest(
            Reservation reservation,
            ReservationRequest request
    ) {
        reservation.setAccommodationId(request.accommodationId());
        reservation.setCheckInDate(request.checkInDate());
        reservation.setCheckOutDate(request.checkOutDate());
        reservation.setGuests(request.guests());
        reservation.setTotalAmount(request.totalAmount());
    }

    private ReservationResponse toResponse(
            Reservation reservation
    ) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUserEmail(),
                reservation.getAccommodationId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getGuests(),
                reservation.getTotalAmount(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}

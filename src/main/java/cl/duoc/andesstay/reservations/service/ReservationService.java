package cl.duoc.andesstay.reservations.service;

import cl.duoc.andesstay.reservations.dto.ReservationRequest;
import cl.duoc.andesstay.reservations.dto.ReservationResponse;
import cl.duoc.andesstay.reservations.entity.Reservation;
import cl.duoc.andesstay.reservations.entity.ReservationStatus;
import cl.duoc.andesstay.reservations.exception.ResourceNotFoundException;
import cl.duoc.andesstay.reservations.messaging.event.ReservationStatusChangedEvent;
import cl.duoc.andesstay.reservations.repository.ReservationRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReservationService(
            ReservationRepository reservationRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }

    /*
     * =========================================================
     * CREAR RESERVA
     * =========================================================
     */

    public ReservationResponse create(
            String userEmail,
            ReservationRequest request
    ) {

        validateDates(request);

        Reservation reservation = new Reservation();

        reservation.setUserEmail(userEmail);

        applyRequest(
                reservation,
                request
        );

        /*
         * Toda reserva nueva comienza en estado CREADA.
         */
        reservation.setStatus(
                ReservationStatus.CREADA
        );

        Reservation saved =
                reservationRepository.save(reservation);

        return toResponse(saved);
    }

    /*
     * =========================================================
     * CAMBIO DE ESTADO
     * =========================================================
     */

    public ReservationResponse updateStatus(
            Long id,
            ReservationStatus newStatus
    ) {

        Reservation reservation =
                findEntityById(id);

        ReservationStatus currentStatus =
                reservation.getStatus();

        /*
         * Validar que el cambio de estado esté permitido.
         */
        validateStatusTransition(
                currentStatus,
                newStatus
        );

        reservation.setStatus(newStatus);

        Reservation saved =
                reservationRepository.save(reservation);

        /*
         * Publicamos un evento interno de Spring.
         *
         * Este evento posteriormente será escuchado por
         * ReservationCommandEventHandler.
         *
         * El handler usará RabbitMQ después del COMMIT.
         */
        eventPublisher.publishEvent(
                new ReservationStatusChangedEvent(
                        saved.getId(),
                        saved.getUserEmail(),
                        saved.getAccommodationId(),
                        saved.getCheckInDate(),
                        saved.getCheckOutDate(),
                        currentStatus,
                        newStatus
                )
        );

        return toResponse(saved);
    }

    /*
     * =========================================================
     * VALIDACIÓN DE TRANSICIONES
     * =========================================================
     */

    private void validateStatusTransition(
            ReservationStatus currentStatus,
            ReservationStatus newStatus
    ) {

        if (currentStatus == newStatus) {

            throw new IllegalStateException(
                    "La reserva ya se encuentra en estado "
                            + newStatus
            );
        }

        boolean validTransition =
                switch (currentStatus) {

                    /*
                     * CREADA
                     *  ├── CONFIRMADA
                     *  └── CANCELADA
                     */
                    case CREADA ->
                            newStatus
                                    == ReservationStatus.CONFIRMADA
                            ||
                            newStatus
                                    == ReservationStatus.CANCELADA;

                    /*
                     * CONFIRMADA
                     *  ├── CHECKIN_PENDIENTE
                     *  └── CANCELADA
                     */
                    case CONFIRMADA ->
                            newStatus
                                    == ReservationStatus.CHECKIN_PENDIENTE
                            ||
                            newStatus
                                    == ReservationStatus.CANCELADA;

                    /*
                     * CHECKIN_PENDIENTE
                     *  ├── EN_ESTADIA
                     *  └── CANCELADA
                     */
                    case CHECKIN_PENDIENTE ->
                            newStatus
                                    == ReservationStatus.EN_ESTADIA
                            ||
                            newStatus
                                    == ReservationStatus.CANCELADA;

                    /*
                     * EN_ESTADIA
                     *  └── CHECKOUT
                     */
                    case EN_ESTADIA ->
                            newStatus
                                    == ReservationStatus.CHECKOUT;

                    /*
                     * Estados terminales.
                     */
                    case CHECKOUT, CANCELADA ->
                            false;
                };

        if (!validTransition) {

            throw new IllegalStateException(
                    "Transición de estado inválida: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }
    }

    /*
     * =========================================================
     * CONSULTAS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<ReservationResponse> findAll() {

        return reservationRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse findById(
            Long id
    ) {

        return toResponse(
                findEntityById(id)
        );
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findByUserEmail(
            String userEmail
    ) {

        return reservationRepository
                .findByUserEmailIgnoreCaseOrderByCreatedAtDesc(
                        userEmail
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * =========================================================
     * MODIFICAR DATOS DE LA RESERVA
     * =========================================================
     */

    public ReservationResponse update(
            Long id,
            ReservationRequest request
    ) {

        validateDates(request);

        Reservation reservation =
                findEntityById(id);

        /*
         * Una reserva cancelada ya no debe modificarse.
         */
        if (
                reservation.getStatus()
                        == ReservationStatus.CANCELADA
        ) {

            throw new IllegalStateException(
                    "No se puede modificar una reserva cancelada"
            );
        }

        /*
         * Una reserva cuyo proceso ya terminó tampoco
         * debería modificarse.
         */
        if (
                reservation.getStatus()
                        == ReservationStatus.CHECKOUT
        ) {

            throw new IllegalStateException(
                    "No se puede modificar una reserva que ya realizó checkout"
            );
        }

        applyRequest(
                reservation,
                request
        );

        Reservation saved =
                reservationRepository.save(reservation);

        return toResponse(saved);
    }

    /*
     * =========================================================
     * MÉTODOS DE COMPATIBILIDAD CON LOS ENDPOINTS ACTUALES
     * =========================================================
     *
     * Estos métodos permiten mantener funcionando:
     *
     * PATCH /api/reservations/{id}/confirm
     * PATCH /api/reservations/{id}/cancel
     *
     * Sin duplicar la lógica.
     */

    public ReservationResponse confirm(
            Long id
    ) {

        return updateStatus(
                id,
                ReservationStatus.CONFIRMADA
        );
    }

    public ReservationResponse cancel(
            Long id
    ) {

        return updateStatus(
                id,
                ReservationStatus.CANCELADA
        );
    }

    /*
     * =========================================================
     * MÉTODOS PRIVADOS
     * =========================================================
     */

    private Reservation findEntityById(
            Long id
    ) {

        return reservationRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "No se encontró la reserva con ID "
                                                + id
                                )
                );
    }

    private void validateDates(
            ReservationRequest request
    ) {

        if (
                !request
                        .checkOutDate()
                        .isAfter(
                                request.checkInDate()
                        )
        ) {

            throw new IllegalArgumentException(
                    "La fecha de salida debe ser posterior a la fecha de entrada"
            );
        }
    }

    private void applyRequest(
            Reservation reservation,
            ReservationRequest request
    ) {

        reservation.setAccommodationId(
                request.accommodationId()
        );

        reservation.setCheckInDate(
                request.checkInDate()
        );

        reservation.setCheckOutDate(
                request.checkOutDate()
        );

        reservation.setGuests(
                request.guests()
        );

        reservation.setTotalAmount(
                request.totalAmount()
        );
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
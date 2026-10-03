package cl.duoc.andesstay.reservations.controller;

import cl.duoc.andesstay.reservations.dto.ReservationRequest;
import cl.duoc.andesstay.reservations.dto.ReservationResponse;
import cl.duoc.andesstay.reservations.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import cl.duoc.andesstay.reservations.dto.ReservationStatusRequest;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(
            ReservationService reservationService
    ) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            JwtAuthenticationToken authentication,
            @Valid @RequestBody ReservationRequest request
    ) {
        String userEmail = getCurrentUserEmail(authentication);

        ReservationResponse response = reservationService.create(
                userEmail,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}/status")
        public ResponseEntity<ReservationResponse> updateStatus(
                @PathVariable Long id,
                @Valid @RequestBody ReservationStatusRequest request
        ) {

        return ResponseEntity.ok(
                reservationService.updateStatus(
                        id,
                        request.status()
                )
        );
        }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> findAll() {
        return ResponseEntity.ok(reservationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(reservationService.findById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponse>> findMine(
            JwtAuthenticationToken authentication
    ) {
        String userEmail = getCurrentUserEmail(authentication);

        return ResponseEntity.ok(
                reservationService.findByUserEmail(userEmail)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(
            JwtAuthenticationToken authentication,
            @PathVariable Long id,
            @Valid @RequestBody ReservationRequest request
    ) {
        validateOwnerOrAdmin(authentication, id);

        return ResponseEntity.ok(
                reservationService.update(id, request)
        );
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ReservationResponse> confirm(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                reservationService.confirm(id)
        );
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancel(
            JwtAuthenticationToken authentication,
            @PathVariable Long id
    ) {
        validateOwnerOrAdmin(authentication, id);

        return ResponseEntity.ok(
                reservationService.cancel(id)
        );
    }

    private void validateOwnerOrAdmin(
            JwtAuthenticationToken authentication,
            Long reservationId
    ) {
        if (isAdmin(authentication)) {
            return;
        }

        String currentUserEmail = getCurrentUserEmail(authentication);

        ReservationResponse reservation =
                reservationService.findById(reservationId);

        if (!reservation.userEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new SecurityException(
                    "No tienes permiso para modificar esta reserva"
            );
        }
    }

    private boolean isAdmin(
            JwtAuthenticationToken authentication
    ) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                );
    }

    private String getCurrentUserEmail(
            JwtAuthenticationToken authentication
    ) {
        String email = authentication
                .getToken()
                .getClaimAsString("preferred_username");

        if (email == null || email.isBlank()) {
            email = authentication
                    .getToken()
                    .getClaimAsString("email");
        }

        if (email == null || email.isBlank()) {
            email = authentication
                    .getToken()
                    .getClaimAsString("upn");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "El token JWT no contiene el correo del usuario"
            );
        }

        return email;
    }
}
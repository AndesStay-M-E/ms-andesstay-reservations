package cl.duoc.andesstay.reservations.dto;

import cl.duoc.andesstay.reservations.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record ReservationStatusRequest(

        @NotNull(message = "El estado es obligatorio")
        ReservationStatus status

) {
}

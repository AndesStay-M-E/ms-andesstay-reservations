package cl.duoc.andesstay.reservations.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationRequest(

        @NotNull(message = "El alojamiento es obligatorio")
        Long accommodationId,

        @NotNull(message = "La fecha de entrada es obligatoria")
        @FutureOrPresent(message = "La fecha de entrada no puede estar en el pasado")
        LocalDate checkInDate,

        @NotNull(message = "La fecha de salida es obligatoria")
        @Future(message = "La fecha de salida debe ser futura")
        LocalDate checkOutDate,

        @NotNull(message = "La cantidad de huéspedes es obligatoria")
        @Min(value = 1, message = "Debe existir al menos un huésped")
        Integer guests,

        @NotNull(message = "El monto total es obligatorio")
        @DecimalMin(
                value = "0.01",
                message = "El monto total debe ser mayor que cero"
        )
        BigDecimal totalAmount
) {
}
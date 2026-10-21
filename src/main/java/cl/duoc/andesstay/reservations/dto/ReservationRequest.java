package cl.duoc.andesstay.reservations.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** totalAmount de entrada es opcional y se ignora: el servidor calcula el valor oficial. */
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

        // Compatibilidad con clientes antiguos; NUNCA utilizar para calcular el total.
        BigDecimal totalAmount
) {}

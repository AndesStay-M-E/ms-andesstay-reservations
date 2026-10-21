package cl.duoc.andesstay.reservations.service;

import cl.duoc.andesstay.reservations.dto.ReservationRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/**
 * El precio unitario y capacidad provienen de ms-andesstay-catalog.
 * Propaga el JWT del usuario, porque el GET del catálogo exige autenticación.
 */
@Component
public class AccommodationPricingClient {
    private final RestClient catalog;

    public AccommodationPricingClient(
            RestClient.Builder builder,
            @Value("${andesstay.services.catalog-url:http://catalog:8082}") String catalogUrl
    ) {
        this.catalog = builder.baseUrl(catalogUrl).build();
    }

    public BigDecimal calculate(ReservationRequest request, String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Falta el token de autenticación para consultar el catálogo");
        }

        AccommodationSnapshot accommodation;
        try {
            accommodation = catalog.get()
                    .uri("/api/catalog/accommodations/{id}", request.accommodationId())
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(AccommodationSnapshot.class);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new IllegalArgumentException("El alojamiento seleccionado no existe");
        } catch (RestClientException exception) {
            throw new IllegalStateException("No se pudo obtener el precio desde el servicio de Catálogo", exception);
        }

        if (accommodation == null || accommodation.id() == null ||
                !accommodation.id().equals(request.accommodationId())) {
            throw new IllegalStateException("Catálogo devolvió un alojamiento inválido");
        }
        if (!accommodation.active()) {
            throw new IllegalArgumentException("El alojamiento seleccionado no está disponible");
        }
        if (accommodation.capacity() == null || request.guests() > accommodation.capacity()) {
            throw new IllegalArgumentException("La cantidad de huéspedes supera la capacidad del alojamiento");
        }
        if (accommodation.pricePerNight() == null ||
                accommodation.pricePerNight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El alojamiento no tiene un precio por noche válido");
        }

        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        if (nights < 1) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la entrada");
        }
        return accommodation.pricePerNight().multiply(BigDecimal.valueOf(nights));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AccommodationSnapshot(
            Long id,
            Integer capacity,
            BigDecimal pricePerNight,
            boolean active
    ) {}
}

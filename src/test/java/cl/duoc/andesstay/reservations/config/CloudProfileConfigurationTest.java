package cl.duoc.andesstay.reservations.config;

import cl.duoc.andesstay.reservations.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el perfil "cloud" toma la conexión desde las variables
 * DB_URL / DB_USERNAME / DB_PASSWORD y que JPA crea la tabla.
 *
 * En la prueba se usa una base en memoria en lugar de la RDS real.
 */
@SpringBootTest(properties = {
        "DB_URL=jdbc:h2:mem:cloudtest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "DB_USERNAME=sa",
        "DB_PASSWORD="
})
@ActiveProfiles("cloud")
class CloudProfileConfigurationTest {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void usaLaConexionDefinidaEnVariablesDeEntorno() {
        assertThat(datasourceUrl).startsWith("jdbc:h2:mem:cloudtest");
        assertThat(reservationRepository.count()).isZero();
    }
}

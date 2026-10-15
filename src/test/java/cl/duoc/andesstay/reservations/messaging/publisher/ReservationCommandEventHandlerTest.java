package cl.duoc.andesstay.reservations.messaging.publisher;

import cl.duoc.andesstay.reservations.entity.ReservationStatus;
import cl.duoc.andesstay.reservations.messaging.config.MessagingProperties;
import cl.duoc.andesstay.reservations.messaging.event.ReservationStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ReservationCommandEventHandlerTest {

    private ReservationCommandPublisher publisher;
    private ReservationCommandEventHandler handler;

    @BeforeEach
    void setUp() {
        MessagingProperties properties = new MessagingProperties(
                new MessagingProperties.Exchanges(
                        "andesstay.commands",
                        "andesstay.events",
                        "andesstay.dlx"
                ),
                new MessagingProperties.Routes(
                        new MessagingProperties.Route("notify.email.q", "email.send", "notify.email.dlq"),
                        new MessagingProperties.Route("catalog.housekeeping.q", "housekeeping.ticket", "catalog.housekeeping.dlq"),
                        new MessagingProperties.Route("report.voucher.q", "voucher.gen", "report.voucher.dlq"),
                        new MessagingProperties.Route("audit.reservation.q", "reservation.#", "audit.reservation.dlq")
                ),
                "reservation."
        );

        publisher = mock(ReservationCommandPublisher.class);
        handler = new ReservationCommandEventHandler(publisher, properties);
    }

    @Test
    void confirmadaPublicaCorreoTicketVoucherYEvento() {
        handler.handle(event(ReservationStatus.CREADA, ReservationStatus.CONFIRMADA));

        verify(publisher).publishEvent(eq("reservation.confirmada"), any());
        verify(publisher).publishCommand(eq("email.send"), any());
        verify(publisher).publishCommand(eq("housekeeping.ticket"), any());
        verify(publisher).publishCommand(eq("voucher.gen"), any());
    }

    @Test
    void creadaSoloPublicaEventoDeAuditoria() {
        handler.handle(event(null, ReservationStatus.CREADA));

        verify(publisher).publishEvent(eq("reservation.creada"), any());
        verify(publisher, never()).publishCommand(any(), any());
    }

    @Test
    void checkoutPublicaCorreoYTicketDeLimpieza() {
        handler.handle(event(ReservationStatus.EN_ESTADIA, ReservationStatus.CHECKOUT));

        verify(publisher, times(1)).publishCommand(eq("email.send"), any());
        verify(publisher, times(1)).publishCommand(eq("housekeeping.ticket"), any());
        verify(publisher, never()).publishCommand(eq("voucher.gen"), any());
    }

    private ReservationStatusChangedEvent event(
            ReservationStatus previous,
            ReservationStatus current
    ) {
        return new ReservationStatusChangedEvent(
                1L,
                "cliente@andesstay.cl",
                10L,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(8),
                previous,
                current
        );
    }
}

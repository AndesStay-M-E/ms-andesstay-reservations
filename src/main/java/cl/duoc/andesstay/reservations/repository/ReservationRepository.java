package cl.duoc.andesstay.reservations.repository;

import cl.duoc.andesstay.reservations.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(
            String userEmail
    );
}

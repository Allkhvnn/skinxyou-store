package skinxyou_backend.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Optional<Order> findByIdempotencyKey(UUID idempotencyKey);

	@Query(value = "select nextval('order_number_seq')", nativeQuery = true)
	long nextOrderNumber();
}

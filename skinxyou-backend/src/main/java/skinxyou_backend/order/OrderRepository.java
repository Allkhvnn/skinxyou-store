package skinxyou_backend.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

	Optional<Order> findByIdempotencyKey(UUID idempotencyKey);

	Optional<Order> findByNumber(String number);

	@Query(value = "select nextval('order_number_seq')", nativeQuery = true)
	long nextOrderNumber();
}

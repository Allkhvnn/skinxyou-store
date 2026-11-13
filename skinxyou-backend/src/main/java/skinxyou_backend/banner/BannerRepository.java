package skinxyou_backend.banner;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BannerRepository extends JpaRepository<Banner, Long> {

	List<Banner> findAllByOrderByPositionAscIdAsc();

	@Query("select coalesce(max(b.position), -1) from Banner b")
	int maxPosition();
}

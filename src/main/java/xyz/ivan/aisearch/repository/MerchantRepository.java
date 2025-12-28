package xyz.ivan.aisearch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.ivan.aisearch.entity.Merchant;

/**
 * @author 15934
 */
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
}

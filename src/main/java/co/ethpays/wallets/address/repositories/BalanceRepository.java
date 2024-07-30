package co.ethpays.wallets.address.repositories;

import co.ethpays.wallets.address.entity.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, String> {
    Balance findByAddress(String address);
    Balance findByUsernameAndCurrencyAndType(String username, String currency, String type);
}
package co.ethpays.wallets.address.repositories;

import co.ethpays.wallets.address.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Integer> {
    Currency findBySymbol(String symbol);
    List<Currency> findByIsDepositAvailable(boolean isDepositAvailable);
}
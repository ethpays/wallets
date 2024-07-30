package co.ethpays.wallets.address.repositories;

import co.ethpays.wallets.address.entity.Currency;
import co.ethpays.wallets.address.entity.Network;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetworkRepository extends JpaRepository<Network, String> {
    List<Network> findByCurrency(Currency currency);
    List<Network> findByName(String name);
}
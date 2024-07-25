package co.ethpays.wallets.address.repositories;

import co.ethpays.wallets.address.entity.Address;
import co.ethpays.wallets.address.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {
    List<Address> findByUserId(String userId);
    Address findByAddress(String address);
    List<Address> findByCurrency(String currency);
    List<Address> findByUserIdAndCurrency(String userId, String currency);
}
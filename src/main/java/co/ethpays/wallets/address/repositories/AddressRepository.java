package co.ethpays.wallets.address.repositories;

import co.ethpays.wallets.address.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {
    Address findByAddress(String address);
}
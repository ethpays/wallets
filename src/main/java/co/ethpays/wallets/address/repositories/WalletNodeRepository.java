package co.ethpays.wallets.address.repositories;

import co.ethpays.wallets.address.entity.WalletNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletNodeRepository extends JpaRepository<WalletNode, UUID> {}
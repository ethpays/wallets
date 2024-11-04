package co.ethpays.wallets.config;

import co.ethpays.wallets.address.entity.WalletNode;
import co.ethpays.wallets.address.repositories.WalletNodeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@AllArgsConstructor
public class NodeManager {
    private final WalletNodeRepository walletNodeRepository;

    public List<WalletNode> getNodes() { return walletNodeRepository.findAll(); }

    public String getNodeById(UUID uuid) {
        WalletNode node = walletNodeRepository.findById(uuid).orElse(null);
        assert node != null;
        return node.getIpv4();
    }
}

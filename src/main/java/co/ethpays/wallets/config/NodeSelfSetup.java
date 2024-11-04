package co.ethpays.wallets.config;

import co.ethpays.wallets.address.entity.WalletNode;
import co.ethpays.wallets.address.repositories.WalletNodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NodeSelfSetup {
    private final WalletNodeRepository walletNodeRepository;
    private final RestTemplate restTemplate;

    @Value("${version}")
    private String version;
    @Value("${nodeId}")
    private String nodeId;
    @Value("${serverPort}")
    private String serverPort;
    @Value("${manualIP}")
    private String manualIP;

    @PostConstruct
    public void setupNode() {
        if (!checkNodeAlreadyExists()) {
            WalletNode node = new WalletNode();
            node.setId(UUID.fromString(nodeId));
            node.setIpv4(getNodeIp());
            node.setPort(serverPort);
            node.setCountry(getCountry());
            node.setTimezone(getTimezone());
            node.setVersion(version);
            walletNodeRepository.save(node);
            logger.info("Node: " + nodeId + "  has been registered successfully.");
        } else {
            WalletNode node = walletNodeRepository.findById(UUID.fromString(nodeId)).orElse(null);
            if (node != null) {
                node.setIpv4(getNodeIp());
                node.setPort(serverPort);
                node.setCountry(getCountry());
                node.setTimezone(getTimezone());
                node.setVersion(version);
                walletNodeRepository.save(node);
                logger.info("Node: " + nodeId + "  has been updated successfully.");
            }
        }
    }

    private String getNodeIp() {
        if (manualIP != null && !manualIP.isEmpty()) {
            return manualIP;
        } else {
            return getPublicIpv4();
        }
    }


    private boolean checkNodeAlreadyExists() {
        if (this.nodeId == null || this.nodeId.isEmpty()) {
            return false;
        }
        UUID nodeId = UUID.fromString(this.nodeId);
        WalletNode node = walletNodeRepository.findById(nodeId).orElse(null);
        return node != null;
    }

    private String getPublicIpv4() {
        String apiUrl = "https://ipapi.co/ip";
        return restTemplate.getForObject(apiUrl, String.class);
    }

    private String getCountry() {
        String apiUrl = "https://ipapi.co/country";
        return restTemplate.getForObject(apiUrl, String.class);
    }

    private String getTimezone() {
        String apiUrl = "https://ipapi.co/timezone";
        return restTemplate.getForObject(apiUrl, String.class);
    }
}

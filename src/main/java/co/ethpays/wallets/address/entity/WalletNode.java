package co.ethpays.wallets.address.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "WalletNodes")
public class WalletNode {
    @Id
    private UUID id;

    private String domain;
    private String description;
    private String ipv4;
    private String ipv6;
    private String port;
    private String publicKey;
    private String status;
    private String country;
    private String timezone;
    private String version;

    public WalletNode() {}

    public WalletNode(String domain, String description, String ipv4, String ipv6, String port, String publicKey, String status, String country, String timezone, String version) {
        this.domain = domain;
        this.description = description;
        this.ipv4 = ipv4;
        this.ipv6 = ipv6;
        this.port = port;
        this.publicKey = publicKey;
        this.status = status;
        this.country = country;
        this.timezone = timezone;
        this.version = version;
    }
}
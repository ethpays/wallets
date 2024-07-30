package co.ethpays.wallets.address.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "currency")
public class Currency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String symbol;
    private String name;
    private String description;
    private String logoUrl;
    private String websiteUrl;
    private String whitepaperUrl;
    private String blockchainExplorerUrl;
    private boolean isDepositAvailable;
}
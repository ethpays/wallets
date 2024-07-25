package co.ethpays.wallets.address.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "depositAddresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String address;
    private String currency;
    private String userId;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'mainnet'")
    private String network = "mainnet";

    private double balance;

    private Timestamp lastSwept;

//    @ManyToOne
//    @JoinColumn(name = "user_id")
//    private User user;
}
package co.ethpays.wallets.address.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}

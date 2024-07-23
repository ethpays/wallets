package co.ethpays.wallets.address.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    private String username;
    private String email;
    private String passwordHash;
    private String passwordSalt;
    private String totpSecret;
    private String role;
    private String status;
    private String allowed_leverage;
    private String commissionLevel;
    private String userLogo;
    private String userLanguage;
    private String userTimezone;

    private boolean disabled;
    private boolean TwoFactorEnabled;

    private Date created_at;
    private Date updated_at;
    private Date blocked_at;

    private double preferredLeverage;
    private double preferredLeverageLong;
    private double preferredLeverageShort;
}

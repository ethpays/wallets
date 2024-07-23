package co.ethpays.wallets.address.managers;

import co.ethpays.wallets.address.entity.User;
import co.ethpays.wallets.address.repositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@AllArgsConstructor
@Component
public class UserManager {
    private final UserRepository userRepository;

    public User getUserByAccessToken(String accessToken) {
        if (accessToken == null) {
            return null;
        }

        String[] parts = accessToken.split(":");
        if (parts.length != 3) {
            return null;
        }

        try {
            User user = userRepository.findById(parts[0]).orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (user == null) {
                return null;
            }
            String accessTokenSecret = generateAccessTokenSecret(user);
            if (!accessTokenSecret.equals(parts[2])) {
                return null;
            }
            return user;
        } catch (Exception e) {
            return null;
        }
    }

    public String generateAccessTokenSecret(User user) {
        String key = user.getUsername() + user.getEmail() + user.getPasswordHash();
        return DigestUtils.md5DigestAsHex(key.getBytes(StandardCharsets.UTF_8));
    }
}

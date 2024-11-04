package co.ethpays.wallets.address.managers;

import co.ethpays.wallets.address.entity.Balance;
import co.ethpays.wallets.address.repositories.BalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BalanceManager {
    private final BalanceRepository balanceRepository;

    public void depositBalance(String username, String currency, double amount, String type) {
        Balance balance = balanceRepository.findByUsernameAndCurrencyAndType(username, currency, type);
        if (balance == null) {
            balance = new Balance();
            balance.setAddress(generateEtpAddress());
            balance.setUsername(username);
            balance.setCurrency(currency);
            balance.setBalance(amount);
            balance.setTotalDeposited(balance.getTotalDeposited() + amount);
            balanceRepository.save(balance);
        } else {
            double newBalance = balance.getBalance() + amount;
            balance.setType(type);
            balance.setBalance(newBalance);
            balanceRepository.save(balance);
        }
    }

    public String generateEtpAddress() {
        StringBuilder etpAddress = new StringBuilder("etp1");
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < 38; i++) {
            int random = (int) (Math.random() * characters.length());
            etpAddress.append(characters.charAt(random));
        }

        Balance toFind = balanceRepository.findByAddress(etpAddress.toString());
        if (toFind != null) {
            generateEtpAddress();
        }
        return etpAddress.toString();
    }
}

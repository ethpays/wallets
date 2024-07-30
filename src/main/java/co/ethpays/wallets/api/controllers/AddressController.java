package co.ethpays.wallets.api.controllers;

import co.ethpays.wallets.address.entity.Address;
import co.ethpays.wallets.address.entity.Currency;
import co.ethpays.wallets.address.entity.User;
import co.ethpays.wallets.address.repositories.AddressRepository;
import co.ethpays.wallets.address.repositories.CurrencyRepository;
import co.ethpays.wallets.address.services.BitcoinService;
import co.ethpays.wallets.address.services.EthereumService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/api/address")
public class AddressController {
    private final BitcoinService bitcoinService;
    private final EthereumService ethereumService;
    private final AddressRepository addressRepository;
    private final CurrencyRepository currencyRepository;

    @GetMapping(value = "/sweepFunds")
    public ResponseEntity<String> sweepFunds(@RequestAttribute(required = false) User currentUser) {
        ethereumService.sweepFunds();
        return ResponseEntity.ok("swept funds");
    }

    @GetMapping(value = "/depositAddresses")
    public List<Address> getDepositAddresses(@RequestAttribute(required = true) User currentUser) {
        return addressRepository.findByUserId(currentUser.getUsername());
    }

    @GetMapping(value = "/getAvailable")
    public List<Currency> getAvailable() {
        return currencyRepository.findByIsDepositAvailable(true);
    }

    @GetMapping(value = "/getAvailableToGenerate")
    public List<Currency> getAvailableToGenerate(@RequestAttribute(required = true) User currentUser) {
        List<Address> alreadyGeneratedAddresses = getDepositAddresses(currentUser);
        List<Currency> availableCurrencies = currencyRepository.findByIsDepositAvailable(true);

        Set<String> alreadyGeneratedCurrencySymbols = alreadyGeneratedAddresses.stream()
                .map(Address::getCurrency)
                .collect(Collectors.toSet());

        List<Currency> currencyMinusAlreadyGenerated = availableCurrencies.stream()
                .filter(currency -> !alreadyGeneratedCurrencySymbols.contains(currency.getSymbol()))
                .collect(Collectors.toList());

        return currencyMinusAlreadyGenerated;
    }

    @PostMapping(value = "/depositAddress")
    public ResponseEntity<String> newDepositAddress(@RequestAttribute(required = true) User currentUser, @RequestParam String currency) {
        String address = null;
        try {
            if (currency.equals("btc")) {
                address = bitcoinService.generateAndStoreAddresses(currentUser);
            } else if(currency.equals("eth")) {
                address = ethereumService.generateAndStoreAddresses(currentUser);
            } else {
                throw new UnsupportedOperationException("Unsupported currency: " + currency);
            }
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error generating deposit address: " + e.getMessage());
        }
    }
}

package co.ethpays.wallets.api.controllers;

import co.ethpays.wallets.address.entity.User;
import co.ethpays.wallets.address.services.BitcoinService;
import co.ethpays.wallets.address.services.EthereumService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/address")
public class AddressController {
    private final BitcoinService bitcoinService;
    private final EthereumService ethereumService;

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

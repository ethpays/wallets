package co.ethpays.wallets.api.controllers;

import co.ethpays.wallets.address.entity.Currency;
import co.ethpays.wallets.address.entity.Network;
import co.ethpays.wallets.address.repositories.CurrencyRepository;
import co.ethpays.wallets.address.repositories.NetworkRepository;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/network")
public class NetworkController {
    private final NetworkRepository networkRepository;
    private final CurrencyRepository currencyRepository;

    @GetMapping(value = "/networks")
    public List<Network> getNetworks(@RequestParam(required = false) String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return networkRepository.findAll();
        } else {
            Currency currencyObj = currencyRepository.findBySymbol(symbol);
            return networkRepository.findByCurrency(currencyObj);
        }
    }
}

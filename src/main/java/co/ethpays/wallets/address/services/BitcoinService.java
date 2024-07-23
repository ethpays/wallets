package co.ethpays.wallets.address.services;

import co.ethpays.wallets.address.entity.User;
import co.ethpays.wallets.address.enums.Currency;
import co.ethpays.wallets.address.repositories.AddressRepository;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.base.Address;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.base.Sha256Hash;
import org.bitcoinj.base.exceptions.AddressFormatException;
import org.bitcoinj.crypto.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class BitcoinService {
    private final AddressRepository addressRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private DeterministicSeed seed;
    private final String SEED_FILE_PATH = "seed.dat";

    public BitcoinService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
        seed = loadSeedFromStorage();
        if (seed == null) {
            byte[] entropy = new byte[16];
            secureRandom.nextBytes(entropy);
            seed = new DeterministicSeed(entropy, "", 0);
            storeSeedToStorage(seed);
        }
    }

    public String generateNewAddress(String username) {
        DeterministicKeyChain keyChain = DeterministicKeyChain.builder().seed(seed).build();
        byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
        byte[] usernameHash = Sha256Hash.hash(usernameBytes);
        int usernameIndex = ByteBuffer.wrap(usernameHash).getInt();

        List<ChildNumber> path = HDPath.parsePath("M/44H/0H/0H/" + usernameIndex);
        DeterministicKey key = keyChain.getKeyByPath(path, true);
        return key.toAddress(ScriptType.P2PKH, MainNetParams.get().network()).toString();
    }

    private DeterministicSeed loadSeedFromStorage() {
        try (BufferedReader reader = new BufferedReader(new FileReader(SEED_FILE_PATH))) {
            String mnemonic = reader.readLine();
            long creationTimeSeconds = Long.parseLong(reader.readLine());
            return new DeterministicSeed(mnemonic, null, "", creationTimeSeconds);
        } catch (IOException e) {
            // Handle the exception, e.g., log the error
            return null;
        }
    }

    private void storeSeedToStorage(DeterministicSeed seed) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SEED_FILE_PATH))) {
            writer.write(String.join(" ", seed.getMnemonicCode()));
            writer.newLine();
            writer.write(Long.toString(seed.getCreationTimeSeconds()));
        } catch (IOException e) {
            // Handle the exception, e.g., log the error
        }
    }

    private void storeAddressToStorage(String address) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("addresses.dat", true))) {
            writer.write(address);
            writer.newLine();
        } catch (IOException e) {
            // Handle the exception, e.g., log the error
        }
    }

    private List<String> loadAddressesFromStorage() {
        List<String> addresses = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("addresses.dat"))) {
            String address;
            while ((address = reader.readLine()) != null) {
                addresses.add(address);
            }
        } catch (IOException e) {
            // Handle the exception, e.g., log the error
        }
        return addresses;
    }

    public String generateAndStoreAddresses(User user) {
        String address = generateNewAddress(user.getUsername());
        if (isValidBitcoinAddress(address)) {
            storeAddressToStorage(address);
            co.ethpays.wallets.address.entity.Address depositAddress = new co.ethpays.wallets.address.entity.Address();
            depositAddress.setAddress(address);
            depositAddress.setCurrency("btc");
            depositAddress.setUser(user);
            addressRepository.save(depositAddress);
        } else {
            logger.error("Generated an invalid Bitcoin Address: " + address);
        }
        return address;
    }

    public void withdrawFunds() {
        List<String> addresses = loadAddressesFromStorage();
        for (String address : addresses) {
            // Implement the withdrawal logic here
            logger.info("Withdrawing from address: " + address);
        }
    }

    public boolean isValidBitcoinAddress(String address) {
        try {
            Address.fromString(MainNetParams.get(), address);
            return true;
        } catch (AddressFormatException e) {
            return false;
        }
    }

}

package co.ethpays.wallets.address.services;

import co.ethpays.wallets.address.entity.User;
import co.ethpays.wallets.address.repositories.AddressRepository;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.base.Sha256Hash;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDPath;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.springframework.stereotype.Service;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.http.HttpService;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class EthereumService {
    private final AddressRepository addressRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private DeterministicSeed seed;
    private final String SEED_FILE_PATH = "eth_seed.dat";
    private final Web3j web3j;

    public EthereumService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
        this.web3j = Web3j.build(new HttpService("https://mainnet.infura.io/v3/2e6b5f18002c4e988f15d92de6309bc0"));
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

        List<ChildNumber> path = HDPath.parsePath("M/44H/60H/0H/0/" + usernameIndex);
        DeterministicKey key = keyChain.getKeyByPath(path, true);
        ECKeyPair ecKeyPair = ECKeyPair.create(key.getPrivKeyBytes());
        return Keys.getAddress(ecKeyPair);
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
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("eth_addresses.dat", true))) {
            writer.write(address);
            writer.newLine();
        } catch (IOException e) {
            // Handle the exception, e.g., log the error
        }
    }

    private List<String> loadAddressesFromStorage() {
        List<String> addresses = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("eth_addresses.dat"))) {
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
        if (isValidEthereumAddress(address)) {
            storeAddressToStorage(address);
            co.ethpays.wallets.address.entity.Address depositAddress = new co.ethpays.wallets.address.entity.Address();
            depositAddress.setAddress(address);
            depositAddress.setCurrency("eth");
            depositAddress.setUser(user);
            addressRepository.save(depositAddress);
        } else {
            logger.error("Generated an invalid Ethereum Address: " + address);
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

    public boolean isValidEthereumAddress(String address) {
        return WalletUtils.isValidAddress(address);
    }

    public BigInteger getBalance(String address) throws ExecutionException, InterruptedException {
        EthGetBalance ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get();
        return ethGetBalance.getBalance();
    }
}

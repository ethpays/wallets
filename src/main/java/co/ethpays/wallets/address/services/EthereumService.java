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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Timestamp;
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
    private final String COLD_WALLET_ADDRESS = "0x126c1742dA49b8E096D3816935FE0547ec083bCc"; // Replace with your actual cold wallet address

    public EthereumService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
        this.web3j = Web3j.build(new HttpService("https://sepolia.infura.io/v3/2e6b5f18002c4e988f15d92de6309bc0"));
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
        List<co.ethpays.wallets.address.entity.Address> existingAddresses = addressRepository.findByUserIdAndCurrency(user.getUsername(), "eth");
        if (!existingAddresses.isEmpty()) {
            return existingAddresses.get(0).getAddress();
        }
        String address = generateNewAddress(user.getUsername());

        if (isValidEthereumAddress(address)) {
            storeAddressToStorage(address);
            co.ethpays.wallets.address.entity.Address depositAddress = new co.ethpays.wallets.address.entity.Address();
            depositAddress.setAddress("0x" + address);
            depositAddress.setCurrency("eth");
            depositAddress.setBalance(0.0000000000000000000);
            depositAddress.setUserId(user.getUsername());
            addressRepository.save(depositAddress);
        } else {
            logger.error("Generated an invalid Ethereum Address: " + address);
        }

        return "0x"+address;
    }

    public boolean isValidEthereumAddress(String address) {
        return WalletUtils.isValidAddress(address);
    }

    @Scheduled(fixedRate = 5000)
    public void checkDeposits() {
        List<co.ethpays.wallets.address.entity.Address> addresses = addressRepository.findByCurrency("eth");
        for (co.ethpays.wallets.address.entity.Address addressEntity : addresses) {
            String address = addressEntity.getAddress();
            try {
                BigInteger balance = getBalance(address);
                double accountBalance = addressEntity.getBalance();
                if (balance.compareTo(BigInteger.ZERO) > 0) {
                    BigDecimal balanceInEther = new BigDecimal(balance).divide(new BigDecimal(Math.pow(10, 18)));
                    if (balanceInEther.doubleValue() > accountBalance) {
                        double depositAmount = balanceInEther.doubleValue() - accountBalance;
                        logger.info("Address " + address + " has received a deposit of " + depositAmount + " ETH. New balance: " + balanceInEther.toString() + " ETH");
                        // Update the balance in your database here
                        addressEntity.setBalance(balanceInEther.doubleValue());
                        addressRepository.save(addressEntity);
                    } else {
                        logger.info("Address " + address + " did not receive any new deposit. Balance: " + balanceInEther.toString() + " ETH");
                    }
                } else {
                    logger.info("Address " + address + " has a balance of 0");
                }
            } catch (ExecutionException | InterruptedException e) {
                logger.error("Error checking balance for address: " + address, e);
            } catch (org.web3j.exceptions.MessageDecodingException e) {
                logger.error("Message decoding error for address: " + address, e);
            } catch (Exception e) {
                logger.error("Unexpected error for address: " + address, e);
            }
        }
    }

    private BigInteger getBalance(String address) throws ExecutionException, InterruptedException {
        EthGetBalance ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get();
        return ethGetBalance.getBalance();
    }

    public void sweepFunds() {
        List<co.ethpays.wallets.address.entity.Address> addresses = addressRepository.findByCurrency("eth");
        for (co.ethpays.wallets.address.entity.Address addressEntity : addresses) {
            String address = addressEntity.getAddress();
            try {
                BigInteger balance = getBalance(address);
                if (balance.compareTo(BigInteger.ZERO) > 0) {
                    sweepAddress(addressEntity, balance);
                }
            } catch (Exception e) {
                logger.error("Error sweeping funds for address: " + address, e);
            }
        }
    }

    private void sweepAddress(co.ethpays.wallets.address.entity.Address addressEntity, BigInteger balance) throws Exception {
        DeterministicKeyChain keyChain = DeterministicKeyChain.builder().seed(seed).build();
        byte[] usernameBytes = addressEntity.getUserId().getBytes(StandardCharsets.UTF_8);
        byte[] usernameHash = Sha256Hash.hash(usernameBytes);
        int usernameIndex = ByteBuffer.wrap(usernameHash).getInt();

        List<ChildNumber> path = HDPath.parsePath("M/44H/60H/0H/0/" + usernameIndex);
        DeterministicKey key = keyChain.getKeyByPath(path, true);
        ECKeyPair ecKeyPair = ECKeyPair.create(key.getPrivKeyBytes());
        Credentials credentials = Credentials.create(ecKeyPair);

        BigInteger gasLimit = BigInteger.valueOf(21000);
        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();

        BigInteger value = balance.subtract(gasPrice.multiply(gasLimit));
        if (value.compareTo(BigInteger.ZERO) > 0) {
            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                    getNonce(credentials.getAddress()), gasPrice, gasLimit, COLD_WALLET_ADDRESS, value);

            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

            if (ethSendTransaction.hasError()) {
                logger.error("Error sending transaction: " + ethSendTransaction.getError().getMessage());
            } else {
                String transactionHash = ethSendTransaction.getTransactionHash();
                logger.info("Transaction successful with hash: " + transactionHash);

                TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 1000, 15);
                TransactionReceipt transactionReceipt = receiptProcessor.waitForTransactionReceipt(transactionHash);
                if (transactionReceipt.isStatusOK()) {
                    addressEntity.setBalance(0.0);
                    addressEntity.setLastSwept(new Timestamp(System.currentTimeMillis()));
                    addressRepository.save(addressEntity);
                } else {
                    logger.error("Transaction failed for address: " + addressEntity.getAddress());
                }
            }
        } else {
            logger.warn("Insufficient funds to cover gas fees for address: " + addressEntity.getAddress());
        }
    }

    private BigInteger getNonce(String address) throws ExecutionException, InterruptedException {
        return web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).sendAsync().get().getTransactionCount();
    }
}
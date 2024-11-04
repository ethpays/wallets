package co.ethpays.wallets.address.services;

import co.ethpays.wallets.address.entity.Address;
import co.ethpays.wallets.address.entity.Transaction;
import co.ethpays.wallets.address.entity.User;
import co.ethpays.wallets.address.managers.BalanceManager;
import co.ethpays.wallets.address.repositories.AddressRepository;
import co.ethpays.wallets.address.repositories.TransactionRepository;
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
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
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
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class EthereumService {
    private final AddressRepository addressRepository;
    private final TransactionRepository transactionRepository;
    private final BalanceManager balanceManager;
    private final SecureRandom secureRandom = new SecureRandom();
    private DeterministicSeed seed;
    private final String SEED_FILE_PATH = "eth_seed.dat";
    private final Web3j web3j;
    private final String COLD_WALLET_ADDRESS = "0x126c1742dA49b8E096D3816935FE0547ec083bCc"; // Replace with your actual cold wallet address

    public EthereumService(AddressRepository addressRepository, TransactionRepository transactionRepository, BalanceManager balanceManager) {
        this.addressRepository = addressRepository;
        this.transactionRepository = transactionRepository;
        this.balanceManager = balanceManager;
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
        String address = generateNewAddress(user.getUsername());

        Address addressToCheck = addressRepository.findByAddress("0x"+address);
        if (addressToCheck == null) {
            if (isValidEthereumAddress(address)) {
                storeAddressToStorage(address);
                Address depositAddress = new Address();
                depositAddress.setAddress("0x" + address);
                depositAddress.setCurrency("eth");
                depositAddress.setBalance(0.0000000000000000000);
                depositAddress.setUserId(user.getUsername());
                addressRepository.save(depositAddress);
            } else {
                logger.error("Generated an invalid Ethereum Address: " + address);
            }

        }
        return "0x"+address;
    }

    public boolean isValidEthereumAddress(String address) {
        return WalletUtils.isValidAddress(address);
    }

    private org.web3j.protocol.core.methods.response.Transaction getLatestTransaction(String address) throws ExecutionException, InterruptedException, IOException {
        BigInteger latestBlockNumber = web3j.ethBlockNumber().send().getBlockNumber();

        for (BigInteger i = latestBlockNumber; i.compareTo(BigInteger.ZERO) >= 0; i = i.subtract(BigInteger.ONE)) {
            EthBlock block = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(i), true).send();
            List<EthBlock.TransactionResult> transactions = block.getBlock().getTransactions();
            for (EthBlock.TransactionResult transactionResult : transactions) {
                org.web3j.protocol.core.methods.response.Transaction transaction = (org.web3j.protocol.core.methods.response.Transaction) transactionResult.get();
                if (transaction.getTo() != null && transaction.getTo().equalsIgnoreCase(address)) {
                    return transaction;
                }
            }
        }
        return null; // Return null if no transaction is found
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
                        addressEntity.setBalance(balanceInEther.doubleValue());

                        org.web3j.protocol.core.methods.response.Transaction latestTransaction = getLatestTransaction(address);
                        String fromAddress = latestTransaction != null ? latestTransaction.getFrom() : "unknown";

                        Transaction depositTransaction = new Transaction();
                        depositTransaction.setAmount(depositAmount);
                        depositTransaction.setUsername(addressEntity.getUserId());
                        depositTransaction.setCurrency(addressEntity.getCurrency());
                        depositTransaction.setFromWallet(fromAddress);
                        depositTransaction.setToWallet(address);
                        depositTransaction.setType("deposit");
                        depositTransaction.setStatus("completed");
                        depositTransaction.setTransactionId(UUID.randomUUID().toString());
                        depositTransaction.setCreatedAt(new Date());
                        depositTransaction.setTitle("Ethereum Deposit");

                        balanceManager.depositBalance(addressEntity.getUserId(), addressEntity.getCurrency(), depositAmount, "futures");

                        transactionRepository.save(depositTransaction);
                        addressRepository.save(addressEntity);
                    }
                }
            } catch (ExecutionException | InterruptedException e) {
                logger.error("Error checking balance for address: " + address, e);
            } catch (org.web3j.exceptions.MessageDecodingException e) {
                logger.error("Message decoding error for address: " + address, e);
            } catch (Exception e) {
                logger.error("Unexpected error for address: " + address, e);
            }
        }
        logger.info("[Ethereum Service]: Watchdog scanned "+addresses.size()+" addresses in total.");
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
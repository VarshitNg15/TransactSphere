package com.transactsphere.account.service;

import com.transactsphere.account.dto.AccountCreateRequest;
import com.transactsphere.account.dto.InternalTransferRequest;
import com.transactsphere.account.model.AccountType;
import com.transactsphere.account.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.cache.CacheManager;

@SpringBootTest
@ActiveProfiles("test")
public class AccountServiceConcurrencyTest {

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean(name = "cacheManager")
    private CacheManager cacheManager;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    private String accountA;
    private String accountB;

    @BeforeEach
    public void setup() {
        // Clear DB
        accountRepository.deleteAll();

        // Create Account A with 15000 balance
        AccountCreateRequest reqA = new AccountCreateRequest();
        reqA.setAccountType(AccountType.SAVINGS);
        accountA = accountService.createAccount(1L, reqA).getAccountNumber();
        
        // Add balance directly
        var accA = accountRepository.findByAccountNumber(accountA).get();
        accA.setBalance(new BigDecimal("15000"));
        accountRepository.save(accA);

        // Create Account B with 10000 balance
        AccountCreateRequest reqB = new AccountCreateRequest();
        reqB.setAccountType(AccountType.SAVINGS);
        accountB = accountService.createAccount(2L, reqB).getAccountNumber();

        var accB = accountRepository.findByAccountNumber(accountB).get();
        accB.setBalance(new BigDecimal("10000"));
        accountRepository.save(accB);
    }

    @AfterEach
    public void tearDown() {
        accountRepository.deleteAll();
    }

    @Test
    public void testHighConcurrencyWithdrawal() throws InterruptedException {
        // Attempting to withdraw 10,000 twice concurrently from Account A
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    InternalTransferRequest transfer = new InternalTransferRequest();
                    transfer.setSourceAccountNumber(accountA);
                    transfer.setAmount(new BigDecimal("10000"));
                    accountService.executeTransfer(transfer);
                } catch (Exception e) {
                    System.out.println("Transfer failed (expected for one thread due to insufficient balance): " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        
        // Only one transaction should succeed. The balance should be 5000.
        BigDecimal finalBalance = accountRepository.findByAccountNumber(accountA).get().getBalance();
        assertEquals(new BigDecimal("5000.00"), finalBalance);
    }

    @Test
    public void testDeadlockPrevention() throws InterruptedException {
        // A -> B and B -> A concurrent transfers
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        Runnable transferAToB = () -> {
            try {
                InternalTransferRequest transfer = new InternalTransferRequest();
                transfer.setSourceAccountNumber(accountA);
                transfer.setTargetAccountNumber(accountB);
                transfer.setAmount(new BigDecimal("1000"));
                accountService.executeTransfer(transfer);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        };

        Runnable transferBToA = () -> {
            try {
                InternalTransferRequest transfer = new InternalTransferRequest();
                transfer.setSourceAccountNumber(accountB);
                transfer.setTargetAccountNumber(accountA);
                transfer.setAmount(new BigDecimal("2000"));
                accountService.executeTransfer(transfer);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        };

        executorService.execute(transferAToB);
        executorService.execute(transferBToA);

        latch.await(10, TimeUnit.SECONDS);

        // Account A: 15000 - 1000 + 2000 = 16000
        // Account B: 10000 + 1000 - 2000 = 9000
        BigDecimal finalBalanceA = accountRepository.findByAccountNumber(accountA).get().getBalance();
        BigDecimal finalBalanceB = accountRepository.findByAccountNumber(accountB).get().getBalance();

        assertEquals(new BigDecimal("16000.00"), finalBalanceA);
        assertEquals(new BigDecimal("9000.00"), finalBalanceB);
    }
}

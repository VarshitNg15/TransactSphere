package com.transactsphere.statement.client;

import com.transactsphere.statement.dto.TransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "transaction-service", url = "http://${TRANSACTION_SERVICE_HOST:localhost}:8084")
public interface TransactionClient {
    @GetMapping("/internal/transactions/account/{accountNumber}")
    List<TransactionResponse> getTransactionsInternal(@PathVariable("accountNumber") String accountNumber);
}

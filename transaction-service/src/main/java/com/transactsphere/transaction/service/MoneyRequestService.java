package com.transactsphere.transaction.service;

import com.transactsphere.transaction.dto.CreateMoneyRequestDto;
import com.transactsphere.transaction.dto.MoneyRequestDto;
import com.transactsphere.transaction.dto.TransactionResponse;
import com.transactsphere.transaction.dto.TransferRequest;
import com.transactsphere.transaction.model.MoneyRequest;
import com.transactsphere.transaction.model.MoneyRequestStatus;
import com.transactsphere.transaction.model.TransactionChannel;
import com.transactsphere.transaction.repository.MoneyRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MoneyRequestService {

    private final MoneyRequestRepository moneyRequestRepository;
    private final TransactionService transactionService;

    @Transactional
    public MoneyRequestDto createMoneyRequest(Long userId, CreateMoneyRequestDto request) {
        MoneyRequest moneyRequest = MoneyRequest.builder()
                .requesterAccountNumber(request.getRequesterAccountNumber())
                .targetUsername(request.getTargetUsername())
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(MoneyRequestStatus.PENDING)
                .build();
        
        moneyRequest = moneyRequestRepository.save(moneyRequest);
        return mapToDto(moneyRequest);
    }

    @Transactional(readOnly = true)
    public List<MoneyRequestDto> getIncomingRequests(String username) {
        return moneyRequestRepository.findByTargetUsernameOrderByCreatedAtDesc(username).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MoneyRequestDto> getOutgoingRequests(Long userId, String accountNumber) {
        return moneyRequestRepository.findByRequesterAccountNumberOrderByCreatedAtDesc(accountNumber).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionResponse acceptMoneyRequest(Long userId, Long requestId, String sourceAccountNumber) {
        MoneyRequest request = moneyRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        
        if (request.getStatus() != MoneyRequestStatus.PENDING) {
            throw new IllegalStateException("Request is already processed");
        }

        TransferRequest transferRequest = TransferRequest.builder()
                .sourceAccountNumber(sourceAccountNumber)
                .targetAccountNumber(request.getRequesterAccountNumber())
                .amount(request.getAmount())
                .description("Accepted money request: " + request.getDescription())
                .channel(TransactionChannel.INTERNAL)
                .build();
        
        TransactionResponse response = transactionService.transfer(userId, "ROLE_USER", transferRequest);
        
        request.setStatus(MoneyRequestStatus.ACCEPTED);
        moneyRequestRepository.save(request);
        
        return response;
    }

    @Transactional
    public MoneyRequestDto rejectMoneyRequest(Long userId, Long requestId) {
        MoneyRequest request = moneyRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        
        if (request.getStatus() != MoneyRequestStatus.PENDING) {
            throw new IllegalStateException("Request is already processed");
        }

        request.setStatus(MoneyRequestStatus.REJECTED);
        request = moneyRequestRepository.save(request);
        return mapToDto(request);
    }

    private MoneyRequestDto mapToDto(MoneyRequest request) {
        return MoneyRequestDto.builder()
                .id(request.getId())
                .requesterAccountNumber(request.getRequesterAccountNumber())
                .targetUsername(request.getTargetUsername())
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }
}

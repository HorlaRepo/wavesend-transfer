package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.dto.BankResolverResponse;
import com.shizzy.moneytransfer.serviceimpl.PayStackServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("paystack")
public class PayStackController {

    private final PayStackServiceImpl payStackServiceImpl;

    @GetMapping("/resolve")
    public Mono<BankResolverResponse> resolveBankAccount(@RequestParam("account_number") String accountNumber,
                                                         @RequestParam("bank_code") String bankCode) {
        return payStackServiceImpl.resolveBankAccount(accountNumber, bankCode);
    }
}

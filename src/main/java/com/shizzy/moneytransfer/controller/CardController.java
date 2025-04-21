package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.GenerateCardRequest;
import com.shizzy.moneytransfer.dto.PinRequest;
import com.shizzy.moneytransfer.model.Card;
import com.shizzy.moneytransfer.service.CardService;
import com.shizzy.moneytransfer.serviceimpl.CardServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("card")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping("/generate")
    public ApiResponse<Card> generateCard(@RequestBody GenerateCardRequest cardRequest) {
        return cardService.generateCard(cardRequest);
    }

    @GetMapping("/{walletId}")
    public ApiResponse<List<Card>> findCardByWalletId(@PathVariable("walletId") Long walletId) {
        return cardService.findCardByWalletId(walletId);
    }

    @PostMapping("/lock/{id}")
    public ApiResponse<String> lockCard(@PathVariable("id") Integer cardId) {
        return cardService.lockCard(cardId);
    }

    @PostMapping("/unlock/{id}")
    public ApiResponse<String> unlockCard(@PathVariable("id") Integer cardId) {
        return cardService.unlockCard(cardId);
    }

    @PutMapping("/set-pin/{id}")
    public ApiResponse<String> setCardPin(@PathVariable("id") Integer cardId, @Valid @RequestBody PinRequest pin) {
        System.out.println(pin.getPin());
        return cardService.setCardPin(cardId, pin.getPin());
    }

    @PostMapping("/verify-pin/{cardId}")
    public boolean verifyPin(@PathVariable Integer cardId, @RequestBody PinRequest pin) {
        return cardService.checkPin(cardId, pin.getPin());
    }

    @DeleteMapping("/delete/{cardId}")
    public ApiResponse<String> deleteCard(@PathVariable Integer cardId) {
        return cardService.deleteCard(cardId);
    }
}

package com.shizzy.moneytransfer.dto;


public record ReceiveMoneyToWalletRequestBody(String mtcn, String securityAnswer) {
}

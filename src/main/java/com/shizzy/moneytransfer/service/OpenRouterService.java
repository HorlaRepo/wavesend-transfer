package com.shizzy.moneytransfer.service;

import java.util.concurrent.CompletableFuture;

public interface OpenRouterService {
    CompletableFuture<String> sendPrompt(String prompt, String model);
}

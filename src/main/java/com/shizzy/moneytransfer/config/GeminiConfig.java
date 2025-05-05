package com.shizzy.moneytransfer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${gemini.api-key}")
    private String apiKey;
    
    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;
    
}
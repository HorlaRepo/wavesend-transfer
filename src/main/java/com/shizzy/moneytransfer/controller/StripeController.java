package com.shizzy.moneytransfer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("stripe")
public class StripeController {
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

}

package com.shizzy.moneytransfer.serviceimpl.factory;

import com.shizzy.moneytransfer.enums.Region;
import com.shizzy.moneytransfer.serviceimpl.builder.AfricaBankAccountBuilder;
import com.shizzy.moneytransfer.serviceimpl.builder.BankAccountBuilder;
import com.shizzy.moneytransfer.serviceimpl.builder.EUBankAccountBuilder;
import com.shizzy.moneytransfer.serviceimpl.builder.USBankAccountBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BankAccountBuilderFactory {
    private final Map<Region, BankAccountBuilder> builders = new HashMap<>();

    @Autowired
    public BankAccountBuilderFactory(AfricaBankAccountBuilder africaBuilder,
                                     EUBankAccountBuilder euBuilder,
                                     USBankAccountBuilder usBuilder) {
        builders.put(Region.AFRICA, africaBuilder);
        builders.put(Region.EU, euBuilder);
        builders.put(Region.US, usBuilder);
    }

    public BankAccountBuilder getBuilder(Region region) {
        BankAccountBuilder builder = builders.get(region);
        if (builder == null) {
            throw new IllegalArgumentException("No builder found for region: " + region);
        }
        return builder;
    }
}

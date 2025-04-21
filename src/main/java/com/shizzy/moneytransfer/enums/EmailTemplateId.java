package com.shizzy.moneytransfer.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EmailTemplateId {

    CREDIT_TRANSACTION("vywj2lpw1dmg7oqz"),
    DEBIT_TRANSACTION("z86org83kk04ew13");

    private final String id;
}

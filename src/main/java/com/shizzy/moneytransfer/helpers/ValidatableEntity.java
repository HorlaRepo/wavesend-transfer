package com.shizzy.moneytransfer.helpers;

import com.shizzy.moneytransfer.model.Admin;

public interface ValidatableEntity {
    String getEmail();
    String getFullName();
    Admin admin();
}

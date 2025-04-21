package com.shizzy.moneytransfer.dto;

import com.shizzy.moneytransfer.validators.ValidBoolean;
import jakarta.validation.constraints.NotNull;

public record UserNotificationPreferencesRequest(

        @NotNull(message = "notifyOnSend is required")
        @ValidBoolean
        boolean notifyOnSend,

        @NotNull(message = "notifyOnReceive is required")
        @ValidBoolean
        boolean notifyOnReceive,

        @NotNull(message = "notifyOnWithdraw is required")
        @ValidBoolean
        boolean notifyOnWithdraw,

        @NotNull(message = "notifyOnDeposit is required")
        @ValidBoolean
        boolean notifyOnDeposit,

        @NotNull(message = "notifyOnPaymentFailure is required")
        @ValidBoolean
        boolean notifyOnPaymentFailure
) {
}

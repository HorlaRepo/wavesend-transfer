package com.shizzy.moneytransfer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@SuperBuilder
public class UserNotificationPreferences extends BaseEntity {
    private boolean notifyOnSend;
    private boolean notifyOnReceive;
    private boolean notifyOnWithdraw;
    private boolean notifyOnDeposit;
    private boolean notifyOnPaymentFailure;
    private boolean notifyOnScheduledTransfers;
    private boolean notifyOnExecutedTransfers;
    private boolean notifyOnCancelledTransfers;

}

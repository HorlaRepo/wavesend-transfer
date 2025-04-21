package com.shizzy.moneytransfer.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransferInfo {
    private String senderId;
    private String receiverId;
    private String senderName;
    private String receiverName;
    private String senderEmail;
    private String receiverEmail;
}

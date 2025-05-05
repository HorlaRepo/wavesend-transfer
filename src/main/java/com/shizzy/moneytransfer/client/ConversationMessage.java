package com.shizzy.moneytransfer.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {
    private String role; 
    private String content;
    
    public static ConversationMessage user(String content) {
        return new ConversationMessage("user", content);
    }
    
    public static ConversationMessage model(String content) {
        return new ConversationMessage("model", content);
    }
}
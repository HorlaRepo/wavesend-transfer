package com.shizzy.moneytransfer.api;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ApiResponse <T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String message;
    private T data;


    // @JsonCreator
    // public ApiResponse(
    //     @JsonProperty("success") boolean success,
    //     @JsonProperty("message") String message,
    //     @JsonProperty("data") T data,
    //     @JsonProperty("localDateTime") LocalDateTime localDateTime) {
            
    //     this.success = success;
    //     this.message = message;
    //     this.data = data;
    // }
}

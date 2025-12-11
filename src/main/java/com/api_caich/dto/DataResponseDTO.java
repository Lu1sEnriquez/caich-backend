package com.api_caich.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Builder;

@Data
@Builder
@EqualsAndHashCode
public class DataResponseDTO<T> {
    private String status;
    private String message;
    private java.time.LocalDateTime timestamp;
    private T data;
    
    public static <T> DataResponseDTO<T> success(T data, String message) {
        return DataResponseDTO.<T>builder()
                .status("SUCCESS")
                .message(message)
                .timestamp(java.time.LocalDateTime.now())
                .data(data)
                .build();
    }
}

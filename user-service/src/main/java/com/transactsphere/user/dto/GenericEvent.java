package com.transactsphere.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenericEvent {
    private Long userId;
    private String message;
}

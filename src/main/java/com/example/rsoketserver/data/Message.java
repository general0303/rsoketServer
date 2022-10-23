package com.example.rsoketserver.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private String origin;
    private String interaction;
    private long index;
    private long created = Instant.now().getEpochSecond();
    private Integer number;

    public Message(String origin, String interaction, Integer number) {
        this.origin = origin;
        this.interaction = interaction;
        this.index = 0;
        this.number = number;
    }

    public Message(String origin, String interaction, long index, Integer number) {
        this.origin = origin;
        this.interaction = interaction;
        this.index = index;
        this.number = number;
    }
}
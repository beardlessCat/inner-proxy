package com.proxy.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Flow implements Serializable {

    private static final long serialVersionUID = 1L;

    private int port;

    private long readBytes;

    private long wroteBytes;

    private long readMsgs;

    private long wroteMsgs;

    private int channels;

    private long timestamp;

}
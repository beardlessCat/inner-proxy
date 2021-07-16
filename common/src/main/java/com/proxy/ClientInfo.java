package com.proxy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientInfo {
    private String pServerHost ;
    private int pServerPort ;
    private String clientId ;
    private String clientSecret ;
    private String exposeServerHost ;
    private int exposeServerPort ;
    private String innerHost ;
    private int innerPort ;
}

package com.proxy;

import io.netty.util.AttributeKey;

public class Constants {
    public static final String AUTH_RESULT_SUCCESS = "1" ;

    public static final String AUTH_RESULT_FAIL = "1" ;

    public static final AttributeKey<Integer> INNER_PORT = AttributeKey.newInstance("inner_port");

    public static final AttributeKey<String> INNER_HOST = AttributeKey.newInstance("inner_host");

    public static final AttributeKey<String> ClIENT_ID = AttributeKey.newInstance("client_id");

    public static final AttributeKey<String> CHANNEL_ID = AttributeKey.newInstance("channel_id");


}

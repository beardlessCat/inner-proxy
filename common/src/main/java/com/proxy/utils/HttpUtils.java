package com.proxy.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;

public class HttpUtils {


    public static FullHttpResponse createFullHttpResponse(HttpVersion httpVersion, HttpResponseStatus status) {
        return createFullHttpResponse(httpVersion, status, null, null, 0);
    }

    public static FullHttpResponse createFullHttpResponse(HttpVersion httpVersion, HttpResponseStatus status,String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ByteBuf content = Unpooled.copiedBuffer(bytes);
        return createFullHttpResponse(httpVersion, status, "text/html; charset=utf-8", content, bytes.length);
    }

    public static FullHttpResponse createJsonFullHttpResponse(HttpVersion httpVersion, HttpResponseStatus status,
        String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ByteBuf content = Unpooled.copiedBuffer(bytes);
        return createFullHttpResponse(httpVersion, status, "application/json; charset=utf-8", content, bytes.length);
    }

    public static FullHttpResponse createFullHttpResponse(HttpVersion httpVersion, HttpResponseStatus status,
        String contentType, ByteBuf body, int contentLength) {
        DefaultFullHttpResponse response;

        if (body != null) {
            response = new DefaultFullHttpResponse(httpVersion, status, body);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        } else {
            response = new DefaultFullHttpResponse(httpVersion, status);
        }

        return response;
    }


}

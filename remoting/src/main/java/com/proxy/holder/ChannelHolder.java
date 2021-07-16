package com.proxy.holder;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;

public class ChannelHolder {

    private static ConcurrentHashMap<Integer, Channel> channels = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, Channel> clientChannels = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, Channel> idChannels = new ConcurrentHashMap<>();

    public static void addChannel(Integer port , Channel channel){
        channels.putIfAbsent(port,channel);
    }

    public static Channel getIIdChannel(String id){
        return idChannels.get(id);
    }

    public static void removeIdChannel(String id){
        idChannels.remove(id);
    }

    public static void addIdChannel(String id , Channel channel){
        idChannels.putIfAbsent(id,channel);
    }

    public static Channel getChannel(Integer port){
        return channels.get(port);
    }

    public static void removeChannel(Integer port){
        channels.remove(port);
    }

    public static void addChannel(String clientId , Channel channel){
        clientChannels.putIfAbsent(clientId,channel);
    }

    public static Channel getChannel(String clientId){
        return clientChannels.get(clientId);
    }

    public static void removeChannel(String clientId){
        channels.remove(clientId);
    }
}

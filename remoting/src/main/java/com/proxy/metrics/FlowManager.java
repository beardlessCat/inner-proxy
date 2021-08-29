package com.proxy.metrics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FlowManager {

    private static Map<Integer, FlowManager> flowManagers = new ConcurrentHashMap<Integer, FlowManager>();

    private Integer port;

    private AtomicLong readBytes = new AtomicLong();

    private AtomicLong wroteBytes = new AtomicLong();

    private AtomicLong readMsgs = new AtomicLong();

    private AtomicLong wroteMsgs = new AtomicLong();

    private AtomicInteger channels = new AtomicInteger();

    private FlowManager() {
    }

    public static FlowManager getCollector(Integer port) {
        FlowManager collector = flowManagers.get(port);
        if (collector == null) {
            synchronized (flowManagers) {
                collector = flowManagers.get(port);
                if (collector == null) {
                    collector = new FlowManager();
                    collector.setPort(port);
                    flowManagers.put(port, collector);
                }
            }
        }

        return collector;
    }

    public static List<Flow> getAndResetAllMetrics() {
        List<Flow> allMetrics = new ArrayList<Flow>();
        Iterator<Entry<Integer, FlowManager>> ite = flowManagers.entrySet().iterator();
        while (ite.hasNext()) {
            allMetrics.add(ite.next().getValue().getAndResetMetrics());
        }

        return allMetrics;
    }

    public static List<Flow> getAllMetrics() {
        List<Flow> allMetrics = new ArrayList<Flow>();
        Iterator<Entry<Integer, FlowManager>> ite = flowManagers.entrySet().iterator();
        while (ite.hasNext()) {
            allMetrics.add(ite.next().getValue().getMetrics());
        }

        return allMetrics;
    }

    public Flow getAndResetMetrics() {
        Flow flow = new Flow();
        flow.setChannels(channels.get());
        flow.setPort(port);
        flow.setReadBytes(readBytes.getAndSet(0));
        flow.setWroteBytes(wroteBytes.getAndSet(0));
        flow.setTimestamp(System.currentTimeMillis());
        flow.setReadMsgs(readMsgs.getAndSet(0));
        flow.setWroteMsgs(wroteMsgs.getAndSet(0));

        return flow;
    }

    public Flow getMetrics() {
        Flow flow = new Flow();
        flow.setChannels(channels.get());
        flow.setPort(port);
        flow.setReadBytes(readBytes.get());
        flow.setWroteBytes(wroteBytes.get());
        flow.setTimestamp(System.currentTimeMillis());
        flow.setReadMsgs(readMsgs.get());
        flow.setWroteMsgs(wroteMsgs.get());

        return flow;
    }

    public void incrementReadBytes(long bytes) {
        readBytes.addAndGet(bytes);
    }

    public void incrementWroteBytes(long bytes) {
        wroteBytes.addAndGet(bytes);
    }

    public void incrementReadMsgs(long msgs) {
        readMsgs.addAndGet(msgs);
    }

    public void incrementWroteMsgs(long msgs) {
        wroteMsgs.addAndGet(msgs);
    }

    public AtomicInteger getChannels() {
        return channels;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

}
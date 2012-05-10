package org.webobjects.distributed;

import com.eaio.uuid.UUIDGen;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
* User: cap_protect
* Date: 5/9/12
* Time: 12:14 PM
*/
public class UniqueClusterName {
    public static final String IN_CLUSTER_ID = detectInClusterId();
    private static final Random nodeNameRandom = new SecureRandom();
    private static final AtomicInteger sequenceNumber = new AtomicInteger();

    public static String get() {
        int seq = sequenceNumber.getAndIncrement();
        if (seq == 0) {
            return IN_CLUSTER_ID;
        }
        return IN_CLUSTER_ID + "$" + seq;
    }

    private static String detectInClusterId() {
        String unique = UUIDGen.getMACAddress();
        if (unique == null) {
            InetAddress addr = null;
            try {
                addr = InetAddress.getLocalHost();
                String hostname = addr.getHostName();
                if (hostname != null) {
                    unique = hostname;
                }
                String hostAddress = addr.getHostAddress();
                if (hostAddress != null) {
                    unique = addr.getHostAddress();
                }
            } catch (UnknownHostException e) {
            }
        }
        if (unique == null) {
            StringBuilder builder = new StringBuilder();
            String abc = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz";
            for (int i = 0; i < 5; i++) {
                builder.append(abc.charAt(nodeNameRandom.nextInt(abc.length())));
            }
            builder.append(nodeNameRandom.nextInt(10000));
            unique = builder.toString();
        }
        long instance;
        try {
            instance = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        } catch(Throwable throwable) {
            instance = nodeNameRandom.nextInt();
        }
        return unique + "-" + instance;
    }


}

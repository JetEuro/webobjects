package distributed;

import registry.Registries;
import registry.RegistryBean;
import registry.RegistryMapType;
import store.RegistryStore;
import store.RegistryStoreUtils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * User: cap_protect
 * Date: 5/9/12
 * Time: 9:30 AM
 */
public class ClusterStateMonitor {
    private static final long DEFAULT_TIMEOUT = 1000L;
    private static final long DEFAULT_CLUSTER_ID = 0;
    private final RegistryStore store;
    private final long timeout;
    private final long pingInterval;
    private final String myNodeName;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final long clusterId;

    private ScheduledFuture<?> checkNodesTaskFuture;
    private ScheduledFuture<?> pingTaskFuture;

    private volatile ClusterState currentClusterState;

    public ClusterStateMonitor(RegistryStore store,
                               long timeout,
                               long pingInterval,
                               long clusterId,
                               String myNodeName) {
        this.store = store;
        this.timeout = timeout;
        this.pingInterval = pingInterval;
        this.myNodeName = myNodeName;
        this.clusterId = clusterId;
        this.currentClusterState = new ClusterState(myNodeName, Collections.singleton(myNodeName));
    }

    public ClusterStateMonitor(RegistryStore store,
                               long timeout,
                               long clusterId,
                               String myNodeName) {
        this(store, timeout, timeout / 2, clusterId, myNodeName);
    }

    public ClusterStateMonitor(RegistryStore store) {
        this(store, DEFAULT_TIMEOUT, DEFAULT_CLUSTER_ID, UniqueClusterName.get());
    }

    public void start() {
        if (pingTaskFuture != null
                || checkNodesTaskFuture != null) {
            throw new IllegalStateException("started");
        }
        PingTask pingTask = new PingTask();
        CheckNodesTask checkTask = new CheckNodesTask();
        pingTask.run();
        checkTask.run();
        checkNodesTaskFuture = scheduledExecutorService.scheduleAtFixedRate(
                checkTask,
                timeout, timeout, TimeUnit.MILLISECONDS);
        pingTaskFuture = scheduledExecutorService.scheduleAtFixedRate(
                pingTask,
                pingInterval, pingInterval, TimeUnit.MILLISECONDS);

    }

    public void stop() {
        if (pingTaskFuture != null) {
            pingTaskFuture.cancel(true);
            pingTaskFuture = null;
        }
        if (checkNodesTaskFuture != null) {
            checkNodesTaskFuture.cancel(true);
            checkNodesTaskFuture = null;
        }
    }

    public ClusterState getState() {
        return currentClusterState;
    }

    public void ping() {
        ClusterStateStored state = Registries.newBean(ClusterStateStored.class);
        state.getNodeStates().put(myNodeName, System.currentTimeMillis());
        RegistryStoreUtils.write(store, clusterId, state);
    }

    public void check() {
        ClusterStateStored value = RegistryStoreUtils.read(store, clusterId, ClusterStateStored.class);
        Set<String> states = new HashSet<String>();
        long time = System.currentTimeMillis();
        for (String name : value.getNodeStates().keySet()) {
            Long timestamp = value.getNodeStates().get(name);
            if (timestamp + timeout > time) {
                states.add(name);
            }
        }
        states.add(myNodeName);
        currentClusterState = new ClusterState(myNodeName, states);
    }


    public interface ClusterStateStored extends RegistryBean {
        @RegistryMapType(String.class)
        Map<String, Long> getNodeStates();
    }

    private class CheckNodesTask implements Runnable {
        public void run() {
            check();
        }
    }

    private class PingTask implements Runnable {
        public void run() {
            ping();
        }
    }

}

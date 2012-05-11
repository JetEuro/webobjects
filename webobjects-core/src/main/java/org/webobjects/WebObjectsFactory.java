package org.webobjects;

import org.webobjects.cassandra.CassandraStore;
import org.webobjects.distributed.*;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import org.webobjects.queue.distributed.DistributedQueue;
import org.webobjects.queue.RegistryBeanQueue;
import org.webobjects.queue.StoredRegistryBeanQueue;
import org.webobjects.queue.distributed.OrderTimeMapDistributedQueue;
import org.webobjects.registry.RegistryBean;
import org.webobjects.store.IdGenerator;
import org.webobjects.store.InMemoryStore;
import org.webobjects.store.RegistryStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: cap_protect
 * Date: 5/10/12
 * Time: 7:48 AM
 */
public abstract class WebObjectsFactory {
    public interface FactoryObject<T> {
        T create();
    }

    public interface StoreFactoryObject extends FactoryObject<RegistryStore> {
        StoreFactoryObject setName(String name);

        StoreFactoryObject setGenerator(IdGenerator generator);

        StoreFactoryObject setGeneratorType(IdGenerator.Type type);
    }

    public interface ClusterStateMonitorFactoryObject extends FactoryObject<ClusterStateMonitor> {
        ClusterStateMonitorFactoryObject setStoreName(String storeName);

        ClusterStateMonitorFactoryObject setStore(RegistryStore store);

        ClusterStateMonitorFactoryObject setTimeout(long timeout);

        ClusterStateMonitorFactoryObject setPingInterval(long pingInterval);

        ClusterStateMonitorFactoryObject setClusterId(long clusterId);

        ClusterStateMonitorFactoryObject setMyNodeName(String myNodeName);
    }

    public interface DistributedQueueFactoryObject<T> extends FactoryObject<DistributedQueue<T>> {
        DistributedQueueFactoryObject<T> setMonitorStoreName(String name);

        DistributedQueueFactoryObject<T> setMonitor(ClusterStateMonitor monitor);

        DistributedQueueFactoryObject<T> setStoreName(String storeName);

        DistributedQueueFactoryObject<T> setStore(RegistryStore store);
    }

    public interface RegistryBeanQueueFactoryObject<T extends RegistryBean>
            extends FactoryObject<RegistryBeanQueue<T>> {
        RegistryBeanQueueFactoryObject<T> setMonitorStoreName(String name);

        RegistryBeanQueueFactoryObject<T> setMonitor(ClusterStateMonitor monitor);

        RegistryBeanQueueFactoryObject<T> setStoreName(String storeName);

        RegistryBeanQueueFactoryObject<T> setStore(RegistryStore store);

        RegistryBeanQueueFactoryObject<T> setIdQueueName(String idQueueName);

        RegistryBeanQueueFactoryObject<T> setIdQueue(DistributedQueue<Long> idQueue);

        <F extends T> RegistryBeanQueueFactoryObject<T> setBeanClass(Class<F> clazz);
    }

    public abstract StoreFactoryObject store();

    public abstract ClusterStateMonitorFactoryObject clusterStateMonitor();

    public abstract <T> DistributedQueueFactoryObject<T> distributedQueue();

    public abstract <T extends RegistryBean> RegistryBeanQueueFactoryObject<T> registryBeanQueue();

    private static final AtomicInteger INTEGER_NAME = new AtomicInteger();

    public static InMemory inMemory() {
        return new InMemory();
    }

    public static Cassandra cassandra(Keyspace keyspace) {
        return new Cassandra(keyspace);
    }

    public static class InMemory extends WebObjectsFactory {
        @Override
        public StoreFactoryObject store() {
            return new InMemoryStoreFactoryObject(InMemory.this);
        }

        @Override
        public ClusterStateMonitorFactoryObject clusterStateMonitor() {
            return new DefaultClusterStateMonitorFactoryObject(InMemory.this);
        }

        @Override
        public <T> DistributedQueueFactoryObject<T> distributedQueue() {
            return new DefaultDistributedQueueFactoryObject<T>(InMemory.this);
        }

        @Override
        public <T extends RegistryBean> RegistryBeanQueueFactoryObject<T> registryBeanQueue() {
            return new DefaultRegistryBeanQueueFactoryObject<T>(InMemory.this);
        }

    }

    public static class Cassandra extends InMemory {
        private Keyspace keyspace;

        private List<ColumnFamilyDefinition> columnDefinitions = new ArrayList();

        public Cassandra(Keyspace keyspace) {
            this.keyspace = keyspace;
        }

        @Override
        public StoreFactoryObject store() {
            return new InMemoryStoreFactoryObject(this) {
                @Override
                public RegistryStore create() {
                    prepare();
                    CassandraStore cassandraStore = new CassandraStore(generator, keyspace, name);
                    columnDefinitions.add(cassandraStore.columnDef());
                    return cassandraStore;
                }
            };
        }

        @Override
        public ClusterStateMonitorFactoryObject clusterStateMonitor() {
            return new DefaultClusterStateMonitorFactoryObject(this);
        }

        @Override
        public <T> DistributedQueueFactoryObject<T> distributedQueue() {
            return new DefaultDistributedQueueFactoryObject<T>(this);
        }

        public Keyspace getKeyspace() {
            return keyspace;
        }

        public List<ColumnFamilyDefinition> getColumnDefinitions() {
            return Collections.unmodifiableList(columnDefinitions);
        }

    }


    private static class InMemoryStoreFactoryObject implements StoreFactoryObject {
        protected final WebObjectsFactory factory;
        protected IdGenerator generator = IdGenerator.getGenerator(IdGenerator.Type.SEQUENTIAL);
        protected String name;

        public InMemoryStoreFactoryObject(WebObjectsFactory factory) {
            this.factory = factory;
        }

        public StoreFactoryObject setName(String name) {
            this.name = name;
            return this;
        }

        public StoreFactoryObject setGenerator(IdGenerator generator) {
            this.generator = generator;
            return this;
        }

        public StoreFactoryObject setGeneratorType(IdGenerator.Type type) {
            this.generator = IdGenerator.getGenerator(type);
            return this;
        }

        public RegistryStore create() {
            prepare();
            return new InMemoryStore(generator);
        }

        protected void prepare() {
            if (name == null) {
                name = Integer.toHexString(INTEGER_NAME.incrementAndGet()).toUpperCase();
            }
            if (generator == null) {
                generator = IdGenerator.getGenerator(IdGenerator.Type.SEQUENTIAL);
            }
        }
    }

    private static class DefaultClusterStateMonitorFactoryObject implements ClusterStateMonitorFactoryObject {
        protected final WebObjectsFactory factory;
        protected RegistryStore store;
        protected long timeout;
        protected long pingInterval;
        protected long clusterId;
        protected String myNodeName;
        protected String storeName;

        public DefaultClusterStateMonitorFactoryObject(WebObjectsFactory factory) {
            this.factory = factory;
        }

        public ClusterStateMonitorFactoryObject setStoreName(String storeName) {
            this.storeName = storeName;
            return this;
        }

        public ClusterStateMonitorFactoryObject setStore(RegistryStore store) {
            this.store = store;
            return this;
        }

        public ClusterStateMonitorFactoryObject setTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public ClusterStateMonitorFactoryObject setPingInterval(long pingInterval) {
            this.pingInterval = pingInterval;
            return this;
        }

        public ClusterStateMonitorFactoryObject setClusterId(long clusterId) {
            this.clusterId = clusterId;
            return this;
        }

        public ClusterStateMonitorFactoryObject setMyNodeName(String myNodeName) {
            this.myNodeName = myNodeName;
            return this;
        }

        public ClusterStateMonitor create() {
            prepare();
            return new ClusterStateMonitor(store, timeout, pingInterval, clusterId, myNodeName);
        }

        protected void prepare() {
            if (storeName == null) {
                storeName = Integer.toHexString(INTEGER_NAME.incrementAndGet()).toUpperCase();
            }
            if (store == null) {
                store = factory.store()
                        .setName(storeName)
                        .create();
            }
            if (timeout <= 0) {
                timeout = ClusterStateMonitor.DEFAULT_TIMEOUT;
            }
            if (timeout < 100) {
                timeout = 100;
            }
            if (pingInterval == 0) {
                pingInterval = timeout / 2;
            }
        }
    }

    private static class DefaultDistributedQueueFactoryObject<T> implements DistributedQueueFactoryObject<T> {
        protected final WebObjectsFactory factory;
        protected ClusterStateMonitor monitor;
        protected String monitorName;
        protected RegistryStore store;
        protected String storeName;

        public DefaultDistributedQueueFactoryObject(WebObjectsFactory factory) {
            this.factory = factory;
        }

        public DistributedQueueFactoryObject<T> setMonitorStoreName(String name) {
            this.monitorName = name;
            return this;
        }

        public DistributedQueueFactoryObject<T> setMonitor(ClusterStateMonitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public DistributedQueueFactoryObject<T> setStoreName(String storeName) {
            this.storeName = storeName;
            return this;
        }

        public DistributedQueueFactoryObject<T> setStore(RegistryStore store) {
            this.store = store;
            return this;
        }

        public DistributedQueue<T> create() {
            prepare();
            return new OrderTimeMapDistributedQueue<T>(store, monitor);
        }

        private void prepare() {
            if (storeName == null) {
                storeName = Integer.toHexString(INTEGER_NAME.incrementAndGet()).toUpperCase();
            }

            if (monitorName == null) {
                monitorName = storeName + "$MONITOR";
            }

            if (monitor == null) {
                monitor = factory.clusterStateMonitor()
                        .setStoreName(monitorName)
                        .create();
            }

            if (store == null) {
                store = factory.store()
                        .setName(storeName)
                        .setGeneratorType(IdGenerator.Type.NANO_TIME)
                        .create();
            }
        }
    }

    private static class DefaultRegistryBeanQueueFactoryObject<T extends RegistryBean> implements RegistryBeanQueueFactoryObject<T> {
        private final WebObjectsFactory factory;
        private String monitorName;
        private ClusterStateMonitor monitor;
        private String storeName;
        private RegistryStore store;
        private String idQueueName;
        private DistributedQueue<Long> idQueue;
        private Class<? extends T> beanClass;

        public DefaultRegistryBeanQueueFactoryObject(WebObjectsFactory factory) {
            this.factory = factory;
        }

        public RegistryBeanQueueFactoryObject<T> setMonitorStoreName(String name) {
            monitorName = name;
            return this;
        }

        public RegistryBeanQueueFactoryObject<T> setMonitor(ClusterStateMonitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public RegistryBeanQueueFactoryObject<T> setStoreName(String storeName) {
            this.storeName = storeName;
            return this;
        }

        public RegistryBeanQueueFactoryObject<T> setStore(RegistryStore store) {
            this.store = store;
            return this;
        }

        public RegistryBeanQueueFactoryObject<T> setIdQueueName(String idQueueName) {
            this.idQueueName = idQueueName;
            return this;
        }

        public RegistryBeanQueueFactoryObject<T> setIdQueue(DistributedQueue<Long> idQueue) {
            this.idQueue = idQueue;
            return this;
        }

        public <F extends T> RegistryBeanQueueFactoryObject<T> setBeanClass(Class<F> clazz) {
            this.beanClass = clazz;
            return this;
        }

        public RegistryBeanQueue<T> create() {
            prepare();
            return new StoredRegistryBeanQueue<T>(store, idQueue, beanClass);
        }

        private void prepare() {
            if (storeName == null) {
                storeName = Integer.toHexString(INTEGER_NAME.incrementAndGet()).toUpperCase();
            }

            if (monitorName == null) {
                monitorName = storeName + "$MONITOR";
            }

            if (idQueueName == null) {
                idQueueName = storeName + "$IDQUEUE";
            }

            if (store == null) {
                store = factory.store()
                        .setName(storeName)
                        .setGeneratorType(IdGenerator.Type.NANO_TIME)
                        .create();
            }

            if (monitor == null) {
                monitor = factory.clusterStateMonitor()
                        .setStoreName(monitorName)
                        .create();
            }

            if (idQueue == null) {
                idQueue = factory.<Long>distributedQueue()
                        .setMonitor(monitor)
                        .setStoreName(idQueueName)
                        .create();
            }

            if (beanClass == null) {
                beanClass = (Class<? extends T>) RegistryBean.class; // for polymorphic behaviour
            }

        }
    }
}

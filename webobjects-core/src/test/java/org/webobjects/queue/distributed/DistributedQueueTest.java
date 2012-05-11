package org.webobjects.queue.distributed;

import jdave.Specification;
import jdave.junit4.JDaveRunner;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftCluster;
import me.prettyprint.hector.api.Cluster;
import org.junit.runner.RunWith;
import org.webobjects.WebObjectsTestCase;
import org.webobjects.distributed.ClusterStateMonitor;
import org.webobjects.store.RegistryStore;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: cap_protect
 * Date: 5/9/12
 * Time: 1:43 PM
 */
@RunWith(JDaveRunner.class)
public class DistributedQueueTest extends Specification<DistributedQueue> {
    public static class LongConsumer implements Runnable {
        private List<Long> values = new ArrayList<Long>();
        private final DistributedQueue<Long> queue;
        private ClusterStateMonitor monitor;
        private final AtomicInteger count;
        private Thread thread;

        public LongConsumer(DistributedQueue<Long> queue, ClusterStateMonitor monitor, AtomicInteger count) {
            this.queue = queue;
            this.monitor = monitor;
            this.count = count;
        }

        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Long val = queue.take();
                    values.add(val);
                    count.incrementAndGet();
                }
            } catch (InterruptedException e) {
                // quit
            }
        }

        public void start() {
            monitor.start();
            thread = new Thread(this);
            thread.start();
        }

        public void stop() {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                // return
            }
            monitor.stop();
        }
    }

    public static class LongProducer implements Runnable {
        private final DistributedQueue<Long> queue;
        private long pos;
        private final ClusterStateMonitor monitor;
        private final long start;
        private final long end;
        private Thread thread;

        public LongProducer(DistributedQueue<Long> queue, ClusterStateMonitor monitor, long start, long end) {
            this.queue = queue;
            this.monitor = monitor;
            this.start = start;
            this.end = end;
            this.pos = start;
        }

        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted() && pos < end) {
                    queue.put(pos++);
                    Thread.sleep(100L);
                }
            } catch (InterruptedException e) {
                // quit
            }
        }

        public void start() {
            monitor.start();
            thread = new Thread(this);
            thread.start();
        }

        public void stop() {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                // return
            }
            monitor.stop();
        }
    }

    public abstract class OfferTakeLong {
        public static final int PRODUCED_COUNT = 10;
        public static final int PRODUCERS = 10;
        public static final int CONSUMERS = 10;
        public static final int TOTAL = PRODUCED_COUNT * PRODUCERS;

        private WebObjectsTestCase testCase = getCase();
        protected abstract WebObjectsTestCase getCase();

        private RegistryStore consumerClusterState = testCase.getFactory().store().create();
        private RegistryStore producerClusterState = testCase.getFactory().store().create();

        private RegistryStore store = testCase.getFactory().store().create();
        private LongConsumer[] consumers;
        private LongProducer[] producers;

        public void offerTake() {
            AtomicInteger count = new AtomicInteger();
            createConsumers(CONSUMERS, count);
            createProducers(PRODUCERS);
            testCase.init();
            startConsumers();
            startProducers();

            System.out.println("GO!");

            int retries = 600;
            int percents = 10;
            while (count.get() < TOTAL && retries-- > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
                if (100 * count.get() / TOTAL >= percents)
                {
                    System.out.println(percents + "%");
                    percents += 10;
                }
            }

            endProducers();
            endConsumers();

            int n = 0;
            Set<Long> result = new TreeSet<Long>();
            int resultCount = 0;
            for (LongConsumer consumer : consumers) {
                System.out.println("consumer#" + (n++) + ": " + consumer.values.size());
                result.addAll(consumer.values);
                resultCount += consumer.values.size();
            }
            specify(count.get(), TOTAL);
            specify(resultCount, TOTAL);
            specify(result.size(), TOTAL);
            for (long i = 0; i < TOTAL; i++) {
                specify(result.contains(i), true);
            }
            testCase.cleanup();
        }


        private void createConsumers(int n, AtomicInteger count) {
            consumers = new LongConsumer[n];
            for (int i = 0; i < consumers.length; i++) {
                ClusterStateMonitor monitor = testCase
                        .getFactory()
                        .clusterStateMonitor()
                        .setStore(consumerClusterState)
                        .create();

                DistributedQueue<Long> queue = testCase
                        .getFactory()
                        .<Long>distributedQueue()
                        .setMonitor(monitor)
                        .setStore(store)
                        .create();
                consumers[i] = new LongConsumer(queue, monitor, count);
            }
        }

        private void createProducers(int n) {
            producers = new LongProducer[n];
            for (int i = 0; i < producers.length; i++) {
                ClusterStateMonitor monitor = testCase
                        .getFactory()
                        .clusterStateMonitor()
                        .setStore(producerClusterState)
                        .create();

                DistributedQueue<Long> queue = testCase
                        .getFactory()
                        .<Long>distributedQueue()
                        .setMonitor(monitor)
                        .setStore(store)
                        .create();
                int start = i * PRODUCED_COUNT;
                int end = (i + 1) * PRODUCED_COUNT;
                producers[i] = new LongProducer(queue, monitor, start, end);
            }
        }

        private void startConsumers() {
            for (LongConsumer consumer : consumers) {
                consumer.start();
            }
            try {
                // establish cluster up
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                return;
            }
            for (LongConsumer consumer : consumers) {
                consumer.monitor.check();
            }
        }

        private void startProducers() {
            for (LongProducer producer : producers) {
                producer.start();
            }
        }

        private void endConsumers() {
            for (LongConsumer consumer : consumers) {
                consumer.stop();
            }
        }

        private void endProducers() {
            for (LongProducer producer : producers) {
                producer.stop();
            }
        }
    }

    public class CassandraOfferTakeLong extends OfferTakeLong {
        private Cluster cluster;

        @Override
        protected WebObjectsTestCase getCase() {
            return WebObjectsTestCase.cassandra(new WebObjectsTestCase.ClusterFactory() {
                public Cluster getCluster() {
                    ThriftCluster localhost = new ThriftCluster("Test Cluster",
                            new CassandraHostConfigurator("localhost"), null);
                    localhost.onStartup();
                    return localhost;
                }
            });
        }
    }

    public class InMemoryOfferTakeLong extends OfferTakeLong {
        @Override
        protected WebObjectsTestCase getCase() {
            return WebObjectsTestCase.inMemory();
        }
    }
}

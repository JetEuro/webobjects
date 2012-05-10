package org.webobjects.distributed;

import jdave.Specification;
import jdave.junit4.JDaveRunner;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.factory.HFactory;
import org.junit.runner.RunWith;
import org.webobjects.CassandraTestCase;
import org.webobjects.InMemoryTestCase;
import org.webobjects.WebObjectsTestCase;
import org.webobjects.store.RegistryStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
        private final AtomicInteger count;
        private Thread thread;

        public LongConsumer(DistributedQueue<Long> queue, AtomicInteger count) {
            this.queue = queue;
            this.count = count;
        }

        public void run() {
            System.out.println("consumer");
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    values.add(queue.take());
                    count.incrementAndGet();
                }
            } catch (InterruptedException e) {
                // quit
            }
        }

        public void start() {
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
        }
    }

    public static class LongProducer implements Runnable {

        private final DistributedQueue<Long> queue;
        private long pos;
        private final long start;
        private final long end;
        private Thread thread;

        public LongProducer(DistributedQueue<Long> queue, long start, long end) {
            this.queue = queue;
            this.start = start;
            this.end = end;
            this.pos = start;
        }

        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted() && pos < end) {
                    queue.put(pos++);
                }
            } catch (InterruptedException e) {
                // quit
            }
        }

        public void start() {
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
        }
    }

    public abstract class OfferTakeElement {
        public static final int PRODUCED_COUNT = 10;
        public static final int PRODUCERS = 10;
        public static final int CONSUMERS = 10;
        public static final int TOTAL = PRODUCED_COUNT * PRODUCED_COUNT;

        private WebObjectsTestCase testCase = getCase();
        protected abstract WebObjectsTestCase getCase();

        private RegistryStore consumerClusterState = testCase.getFactory().store().create();
        private RegistryStore producerClusterState = testCase.getFactory().store().create();

        private RegistryStore store = testCase.getFactory().store().create();
        private LongConsumer[] consumers;
        private LongProducer[] producers;

        public void offerTake() {
            AtomicInteger count = new AtomicInteger();
            initConsumers(CONSUMERS, count);
            initProducers(PRODUCERS);
            testCase.init();
            startConsumers();
            startProducers();

            int retries = 500;
            while (count.get() < TOTAL && retries-- > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
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

        private void startConsumers() {
            for (LongConsumer consumer : consumers) {
                ClusterStateMonitor monitor = consumer.queue.getMonitor();
                monitor.start();
                monitor.check();
                consumer.start();
            }
        }

        private void startProducers() {
            for (LongProducer producer : producers) {
                ClusterStateMonitor monitor = producer.queue.getMonitor();
                monitor.start();
                monitor.check();
                producer.start();
            }
        }

        private void endConsumers() {
            for (LongConsumer consumer : consumers) {
                consumer.stop();
            }
        }

        private void initConsumers(int n, AtomicInteger count) {
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
                consumers[i] = new LongConsumer(queue, count);
            }
        }

        private void initProducers(int n) {
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
                producers[i] = new LongProducer(queue, start, end);
            }
            for (LongProducer producer : producers) {
                producer.queue.getMonitor().check();
            }
        }

        private void endProducers() {
            for (LongProducer producer : producers) {
                producer.stop();
            }
        }
    }

    public class InMemoryOfferTakeElement extends OfferTakeElement {
        @Override
        protected WebObjectsTestCase getCase() {
            return new InMemoryTestCase();
        }
    }

    public class CassandraOfferTakeElement extends OfferTakeElement {
        private Cluster cluster;

        @Override
        protected WebObjectsTestCase getCase() {
            if (cluster == null) {
                cluster = HFactory.getOrCreateCluster("Test Cluster", "localhost");
            }
            return new CassandraTestCase(cluster);
        }
    }

}

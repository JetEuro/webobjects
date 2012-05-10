package org.webobjects.distributed;

import org.webobjects.registry.Registries;
import org.webobjects.registry.RegistryBean;
import org.webobjects.registry.RegistryMapType;
import org.webobjects.store.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * User: TCSDEVELOPER
 * Date: 5/9/12
 * Time: 1:00 PM
 */
public class DistributedQueue<T> extends AbstractList<T> implements BlockingQueue<T> {
    private final RegistryStore store;
    private final IdRangeIterable idIterable;
    private final IdGenerator timeGenerator = IdGenerator.getGenerator(IdGenerator.Type.NANO_TIME);
    private final ClusterStateMonitor monitor;
    private long fetchTime = 0;
    private long outdateInterval = 1000;
    private final Random random = new SecureRandom();

    public DistributedQueue(RegistryStore store, ClusterStateMonitor monitor) {
        this.store = store;
        this.monitor = monitor;
        if (!(store instanceof IdRangeIterable)) {
            throw new UnsupportedOperationException("store does not support IdRangeIterable");
        }
        idIterable = (IdRangeIterable) store;
    }

    public ClusterStateMonitor getMonitor() {
        return monitor;
    }

    public RegistryStore getStore() {
        return store;
    }

    public long getOutdateInterval() {
        return outdateInterval;
    }

    public void setOutdateInterval(long outdateInterval) {
        this.outdateInterval = outdateInterval;
    }

    public interface QueueElement<T> extends RegistryBean
    {
        T getValue();

        void setValue(T value);

        boolean isFetched();

        void setFetched(boolean value);

        @RegistryMapType(Long.class)
        Map<Long, Long> getOrder();
    }

    @Override
    public boolean add(T t) {
        QueueElement<T> element = Registries.newBean(QueueElement.class);
        element.setValue(t);
        element.setFetched(false);
        int nstates = monitor.getState().getStates().size() * 2;
        Map<Long, Long> order = element.getOrder();
        long time = System.currentTimeMillis();
        while (nstates-- > 0) {
            order.put(time, random.nextLong());
            time += outdateInterval;
        }
        RegistryStoreUtils.write(store, timeGenerator.newId(), element);
        return true;
    }

    public void put(T t) throws InterruptedException {
        add(t);
    }

    public boolean offer(T t) {
        return add(t);
    }

    public boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {
        return offer(t);
    }

    @Override
    public T get(int index) {
        return null;
    }

    @Override
    public int size() {
        return 1;
    }

    public T remove() {
        IdRangeIterator idSelector = idIterable.getIdRangeSelector(fetchTime, null);
        long[] times;
        while ((times = idSelector.next(100)).length != 0) {
            ClusterState state = monitor.getState();
            String myName = state.getMe().getName();
            int states = state.getStates().size();
            int myId = state.getMe().getId();
            for (long id : times) {
                BeanStorer beanStorer = store.newStorer(Registries.newRegistry());
                beanStorer.setId(id);
                beanStorer.load();

                QueueElement<T> element = beanStorer.getBean(QueueElement.class);
                if (element.isFetched()) {
                    continue;
                }
                Map<Long, Long> order = element.getOrder();
                Map<Long, Long> sortedOrder = new TreeMap<Long, Long>(order);
                long time = System.currentTimeMillis();
                for (Long key : sortedOrder.keySet()) {
                    long someId = Math.abs(sortedOrder.get(key)) % states;
                    if (key >= time && someId == myId) {
                        element.setFetched(true);
                        beanStorer.store();
                        return element.getValue();
                    }
                }
            }
        }
        throw new NoSuchElementException("queue empty");
    }


    private boolean isOutdated(long time) {
        return time + outdateInterval > System.nanoTime();
    }


    public T poll() {
        return null;
    }

    public T element() {
        return null;
    }

    public T peek() {
        return null;
    }

    public T take() throws InterruptedException {
        while (true) {
            IdRangeIterator idSelector = idIterable.getIdRangeSelector(fetchTime, null);
            long[] times;
            while ((times = idSelector.next(100)).length != 0) {
                ClusterState state = monitor.getState();
                String myName = state.getMe().getName();
                int states = state.getStates().size();
                int myId = state.getMe().getId();
                boolean allFetched = true;
                for (long id : times) {
                    BeanStorer beanStorer = store.newStorer(Registries.newRegistry());
                    beanStorer.setId(id);
                    beanStorer.load();

                    QueueElement<T> element = beanStorer.getBean(QueueElement.class);
                    if (element.isFetched()) {
                        continue;
                    }
                    allFetched = false;
                    Map<Long, Long> order = element.getOrder();
                    Map<Long, Long> sortedOrder = new TreeMap<Long, Long>(order);
                    long time = System.nanoTime();
                    List<Long> keys = new ArrayList<Long>(sortedOrder.keySet());
                    for (int i = 0; i < keys.size(); i++) {
                        Long elTime = keys.get(i);
                        Long nextTime = i + 1 < keys.size() ? keys.get(i + 1) : null;
                        if (elTime <= time) {
                            if (nextTime == null || time < nextTime) {
                                long someId = Math.abs(sortedOrder.get(elTime)) % states;
                                if (someId == myId) {
                                    element.setFetched(true);
                                    beanStorer.store();
                                    return element.getValue();
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
                if (allFetched) {
                    fetchTime = times[times.length - 1] + 1;
                }
            }
            Thread.sleep(100L);
        }
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    public int remainingCapacity() {
        return 0;
    }

    public int drainTo(Collection<? super T> c) {
        return 0;
    }

    public int drainTo(Collection<? super T> c, int maxElements) {
        return 0;
    }
}

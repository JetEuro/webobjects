package org.webobjects.queue.distributed;

import org.webobjects.distributed.ClusterState;
import org.webobjects.distributed.ClusterStateMonitor;
import org.webobjects.registry.Registries;
import org.webobjects.registry.RegistryBean;
import org.webobjects.registry.RegistryGettable;
import org.webobjects.registry.RegistryMapType;
import org.webobjects.store.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * User: cap_protect
 * Date: 5/9/12
 * Time: 1:00 PM
 */
public class OrderTimeMapDistributedQueue<T> implements DistributedQueue<T> {
    public static final int SLEEP_EACH_ELEMENTS = 100;
    private final RegistryStore store;
    private final IdRangeIterable idIterable;
    private final IdGenerator timeGenerator = IdGenerator.getGenerator(IdGenerator.Type.NANO_TIME);
    private final ClusterStateMonitor monitor;
    private long outdateInterval = 5000;
    private final Random random = new SecureRandom();
    private IdRangeIterator idSelector;

    public OrderTimeMapDistributedQueue(RegistryStore store, ClusterStateMonitor monitor) {
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

    public interface QueueElement<T> extends RegistryBean, RegistryGettable
    {
        T getValue();

        void setValue(T value);

        boolean isFetched();

        void setFetched(boolean value);

        boolean isNotProcessed();

        void setNotProcessed(boolean value);

        @RegistryMapType(Long.class)
        Map<Long, Long> getOrder();
    }

    public boolean add(T t) {
        QueueElement<T> element = Registries.newBean(QueueElement.class);
        element.setValue(t);
        element.setFetched(false);
        element.setNotProcessed(true);
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

    public int size() {
        throw new UnsupportedOperationException("size");
    }

    public T remove() {
        return takeNextElementDetermined(true);
    }

    private boolean isOutdated(long time) {
        return time + outdateInterval > System.nanoTime();
    }

    public T poll() {
        return takeNextElementDetermined(true);
    }

    public T element() {
        T object = peek();
        if (object == null) {
            throw new NoSuchElementException("queue is empty");
        }
        return object;
    }

    public T peek() {
        return takeNextElementDetermined(false);
    }

    public T take() throws InterruptedException {
        while (true) {
            T value = takeNextElement(true, false);
            if (value != null) {
                return value;
            }
            Thread.sleep(100L);
        }
    }

    private T takeNextElement(boolean fetch, boolean determined) throws InterruptedException {
        if (idSelector == null) {
            newSelector();
        }

        int count = 0;
        while (idSelector.hasNext()) {
            Long peek = idSelector.peek();
            T value = takeById(peek, true);
            if (value != null) {
                return value;
            }
            if (count % SLEEP_EACH_ELEMENTS == 0) {
                if (!determined) {
                    Thread.sleep(50L);
                }
            }
            count++;
        }
        newSelector();
        return null;
    }

    private T takeNextElementDetermined(boolean fetch) {
        try {
            return takeNextElement(fetch, true);
        } catch (InterruptedException e) {
            return null; // never happens
        }
    }

    private T takeById(long id, boolean fetch) {
        BeanStorer beanStorer = store.newStorer(Registries.newRegistry());
        beanStorer.setId(id);
        beanStorer.load();

        QueueElement<T> element = beanStorer.getBean(QueueElement.class);
        if (element.isFetched()) {
            return null;
        }
        T value = takeElement(beanStorer, element, fetch);
        if (value != null) {
            return value;
        }
        return null;
    }

    private T takeElement(BeanStorer beanStorer, QueueElement<T> element, boolean fetch) {
        ClusterState state = monitor.getState();
        int states = state.getStates().size();
        int myId = state.getMe().getId();
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
//                    System.out.println("CMP " + someId + "==" + myId
//                            + (someId == myId ? "! " : " ") + element.getValue() + " " + states);
                    if (someId == myId) {
                        if (fetch) {
                            idSelector.next();
                            element.setFetched(true);
                            beanStorer.addRemovedKey("notProcessed");
                            beanStorer.store();
                        }
                        return element.getValue();
                    } else {
                        break;
                    }
                }
            }
        }
        return null;
    }

    private void newSelector() {
        idSelector = idIterable.getIdRangeSelector(null, null).addFilter("notProcessed");
    }

    public boolean isEmpty() {
        return takeNextElementDetermined(false) == null;
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        long start = System.currentTimeMillis();
        long timeoutMs = unit.toMillis(timeout);

        while (true) {
            T element = takeNextElement(false, false);
            long current = System.currentTimeMillis();
            long last = current - start;
            if (element != null) {
                return element;
            }
            if (last > timeoutMs) {
                return null;
            }
            Thread.sleep(100L);
        }
    }

    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException("remove");
    }

    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("containsAll");
    }

    public boolean addAll(Collection<? extends T> c) {
        boolean changed = false;
        for (T obj : c) {
            if (add(obj)) {
                changed = true;
            }
        }
        return changed;
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("removeAll");
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("retainAll");
    }

    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    public boolean contains(Object o) {
        throw new UnsupportedOperationException("contains");
    }

    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("iterator");
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException("toArray");
    }

    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("toArray");
    }

    public int drainTo(Collection<? super T> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    public int drainTo(Collection<? super T> c, int maxElements) {
        T t;
        int count = 0;
        while ((t = poll()) != null && maxElements-- > 0) {
            c.add(t);
            count++;
        }
        return count;
    }
}

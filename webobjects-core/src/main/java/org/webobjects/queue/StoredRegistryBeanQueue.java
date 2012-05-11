package org.webobjects.queue;

import org.webobjects.registry.Registries;
import org.webobjects.registry.Registry;
import org.webobjects.registry.RegistryBean;
import org.webobjects.registry.RegistryGettable;
import org.webobjects.store.RegistryStore;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 12:31 PM
 */
public class StoredRegistryBeanQueue<T extends RegistryBean> implements RegistryBeanQueue<T> {
    private final RegistryStore store;
    private BlockingQueue<Long> idQueue;
    private Class<T> clazz;

    public <F extends T> StoredRegistryBeanQueue(RegistryStore store, BlockingQueue<Long> idQueue, Class<F> clazz) {
        this.store = store;
        this.idQueue = idQueue;
        this.clazz = (Class) clazz;
    }

    public StoredRegistryBeanQueue(RegistryStore store, BlockingQueue<Long> idQueue) {
        this(store, idQueue, (Class) RegistryBean.class);
    }

    public RegistryStore getStore() {
        return store;
    }

    public BlockingQueue<Long> getIdQueue() {
        return idQueue;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public boolean add(T t) {
        Long id = save(t);
        return idQueue.add(id);
    }

    public boolean offer(T t) {
        Long id = save(t);
        return idQueue.offer(id);
    }

    private Long save(T t) {
        RegistryGettable gettable = (RegistryGettable) t;
        return store
                .newStorer(gettable.getRegistry())
                .renewId()
                .store().getId();
    }

    public void put(T t) throws InterruptedException {
        idQueue.put(save(t));
    }

    public boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {
        return idQueue.offer(save(t), timeout, unit);
    }

    public T take() throws InterruptedException {
        return load(idQueue.take());
    }

    private T load(long id) {
        Registry registry = Registries.newRegistry();
        store
                .newStorer(registry)
                .setId(id)
                .load();
        return registry.bean(clazz);
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return load(idQueue.poll(timeout, unit));
    }

    public int remainingCapacity() {
        return idQueue.remainingCapacity();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException("remove");
    }

    public boolean contains(Object o) {
        throw new UnsupportedOperationException("remove");
    }

    public int drainTo(Collection<? super T> c) {
        throw new UnsupportedOperationException("drainTo");
    }

    public int drainTo(Collection<? super T> c, int maxElements) {
        throw new UnsupportedOperationException("drainTo");
    }

    public T remove() {
        Long value = idQueue.remove();
        if (value == null) {
            return null;
        }
        return load(value);
    }

    public T poll() {
        Long value = idQueue.poll();
        if (value == null) {
            return null;
        }
        return load(value);
    }

    public T element() {
        return load(idQueue.element());
    }

    public T peek() {
        Long value = idQueue.peek();
        if (value == null) {
            return null;
        }
        return load(value);
    }

    public int size() {
        return idQueue.size();
    }

    public boolean isEmpty() {
        return idQueue.isEmpty();
    }

    public Iterator<T> iterator() {
        return new LoadingItertor(idQueue.iterator());
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException("toArray");
    }

    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("toArray");
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
        return true;
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("removeAll");
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("retainAll");
    }

    public void clear() {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public boolean equals(Object o) {
        return idQueue.equals(o);
    }

    @Override
    public int hashCode() {
        return idQueue.hashCode();
    }

    private class LoadingItertor implements Iterator<T> {
        private final Iterator<Long> iterator;

        public LoadingItertor(Iterator<Long> iterator) {
            this.iterator = iterator;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public T next() {
            return load(iterator.next());
        }

        public void remove() {
            iterator().remove();
        }
    }
}

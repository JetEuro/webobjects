package org.webobjects.store;

import org.webobjects.registry.Registries;
import org.webobjects.registry.Registry;
import org.webobjects.registry.RegistryBean;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 8:26 PM
 */
public class InMemoryStore implements RegistryStore, IdRangeIterable {
    private IdGenerator generator;
    private Object lock = new Object();

    public InMemoryStore(IdGenerator generator) {
        this.generator = generator;
    }

    private SortedMap<Long, Map<String, Object>> backendMap;
    {
        backendMap = new ConcurrentSkipListMap<Long, Map<String, Object>>();
    }
    public long newId() {
        return generator.newId();
    }

    public BeanStorer newStorer(Registry registry) {
        return new InMemoryBeanStorer(registry);
    }

    public void store(long id, Registry registry, String[] path, List<String> keysToRemove) {
        Map<String, Object> liniarizedData = Registries.liniarize(registry, path);
        Map<String, Object> data;
        synchronized (lock) {
            data = backendMap.get(id);
            if (data == null) {
                backendMap.put(id, liniarizedData);
            }
        }
        if (data != null) {
            synchronized (data) {
                data.putAll(liniarizedData);
                for (String key : keysToRemove) {
                    data.remove(key);
                }
            }
        }
    }

    public boolean load(long id, Registry registry, String[] path) {
        Map<String, Object> map = backendMap.get(id);
        if (map == null) {
            return false;
        }
        synchronized (map) {
            Registries.putMassivlyWithPrefix(registry, map, path);
        }
        return true;
    }

    public SimpleIdRangeIterator getIdRangeSelector(Long start, Long end) {
        SortedMap<Long, Map<String, Object>> map = backendMap;
        if (start != null) {
            map = map.tailMap(start);
        }
        if (end != null) {
            map = map.headMap(end);
        }
        final Iterator<Map.Entry<Long, Map<String, Object>>> iterator;
        iterator = map.entrySet().iterator();

        return new SimpleIdRangeIterator(iterator);
    }

    private static class SimpleIdRangeIterator implements org.webobjects.store.IdRangeIterator {
        private Set<String> filters;
        public Long peekedValue;
        private final Iterator<Map.Entry<Long, Map<String, Object>>> iterator;

        public SimpleIdRangeIterator(Iterator<Map.Entry<Long, Map<String, Object>>> iterator) {
            this.iterator = iterator;
            filters = new HashSet();
        }

        public Long next() {
            long value = peek();
            peekedValue = null;
            return value;
        }

        public Long peek() {
            if (peekedValue != null) {
                return peekedValue;
            }
            while (iterator.hasNext()) {
                Map.Entry<Long, Map<String, Object>> entry = iterator.next();
                if (!filters.isEmpty()) {
                    Map<String, Object> map = entry.getValue();
                    synchronized (map) {
                        if (!map.keySet().containsAll(filters)) {
                            continue;
                        }
                    }
                }
                peekedValue = entry.getKey();
                break;
            }
            return peekedValue;
        }

        public org.webobjects.store.IdRangeIterator addFilter(String... pathes) {
            filters.addAll(Arrays.asList(pathes));
            return this;
        }

        public boolean hasNext() {
            return peek() != null;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }

    private class InMemoryBeanStorer implements BeanStorer {
        Long id;
        private final Registry registry;
        private final String[] path;
        private List<String> keysToRemove = new ArrayList<String>();

        public InMemoryBeanStorer(Registry registry, String[] path) {
            this.registry = registry;
            id = null;
            this.path = path;
        }

        public InMemoryBeanStorer(Registry registry) {
            this(registry, new String[0]);
        }

        public <T extends RegistryBean> T getBean(Class<T> clazz) {
            return registry.bean(clazz);
        }

        public BeanStorer subStorer(String... path) {
            List<String> result = new ArrayList<String>(this.path.length + path.length);
            result.addAll(Arrays.asList(this.path));
            result.addAll(Arrays.asList(path));
            String[] resultArr = result.toArray(new String[result.size()]);
            return new InMemoryBeanStorer(registry, resultArr);
        }

        public void addRemovedKey(String key) {
            keysToRemove.add(key);
        }

        public BeanStorer renewId() {
            id = InMemoryStore.this.newId();
            return this;
        }

        public long getId() {
            return id;
        }

        public BeanStorer setId(long id) {
            this.id = id;
            return this;
        }

        public boolean load() {
            if (id == null) {
                throw new IllegalStateException("id not set");
            }
            keysToRemove.clear();
            return InMemoryStore.this.load(id, registry, path);
        }

        public BeanStorer store() {
            if (id == null) {
                renewId();
            }
            InMemoryStore.this.store(id, registry, path, keysToRemove);
            keysToRemove.clear();
            return this;
        }

        public Registry getRegistry() {
            return registry;
        }

        public void dispose() {
            // do nothing
        }
    }
}

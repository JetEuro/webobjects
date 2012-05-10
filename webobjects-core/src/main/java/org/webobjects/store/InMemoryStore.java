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

    public void store(long id, Registry registry, String[] path) {
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

    public IdRangeIterator getIdRangeSelector(Long start, Long end) {
        SortedMap<Long, Map<String, Object>> map = backendMap;
        if (start != null) {
            map = map.tailMap(start);
        }
        if (end != null) {
            map = map.headMap(end);
        }
        final Iterator<Long> iterator;
        iterator = map.keySet().iterator();

        return new IdRangeIterator() {
            public long[] next(int count) {
                long []ids = new long[count];
                int i = 0;
                while (count-- > 0 && iterator.hasNext()) {
                    ids[i++] = iterator.next();
                }
                long []ret = new long [i];
                System.arraycopy(ids, 0, ret, 0, ret.length);
                return ret;
            }
        };
    }

    private class InMemoryBeanStorer implements BeanStorer {
        Long id;
        private final Registry registry;
        private final String[] path;

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
            return InMemoryStore.this.load(id, registry, path);
        }

        public BeanStorer store() {
            if (id == null) {
                renewId();
            }
            InMemoryStore.this.store(id, registry, path);
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

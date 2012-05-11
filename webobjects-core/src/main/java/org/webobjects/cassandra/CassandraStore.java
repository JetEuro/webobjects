package org.webobjects.cassandra;

import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.ObjectSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.template.ColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.ThriftColumnFamilyTemplate;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.SliceQuery;
import org.webobjects.registry.Registries;
import org.webobjects.registry.Registry;
import org.webobjects.registry.RegistryBean;
import org.webobjects.store.*;

import java.util.*;

/**
 * User: cap_protect
 * Date: 5/8/12
 * Time: 10:45 AM
 */
public class CassandraStore implements RegistryStore, IdRangeIterable {
    private static final LongSerializer KEY_SERIALIZER = LongSerializer.get();
    private static final StringSerializer NAME_SERIALIZER = StringSerializer.get();
    private static final ObjectSerializer VALUE_SERIALIZER = ObjectSerializer.get();
    private final IdGenerator generator;
    private final Keyspace keyspace;
    private final String columnFamily;
    private final ColumnFamilyTemplate<Long, String> template;

    public CassandraStore(IdGenerator generator, Keyspace keyspace, String columnFamily) {
        this.generator = generator;
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;
        template = new ThriftColumnFamilyTemplate<Long, String>(keyspace,
                columnFamily, KEY_SERIALIZER, NAME_SERIALIZER);
    }

    public long newId() {
        return generator.newId();
    }

    public BeanStorer newStorer(final Registry registry) {
        return new CassandraBeanStorer(registry);
    }

    public void store(long id, Registry registry, List<String> keysToRemove) {
        Mutator<Long> mutator = template.createMutator();
        Map<String, Object> properties = Registries.liniarize(registry, new String[0]);
        for (String key : properties.keySet()) {
            Object value = properties.get(key);
            HColumn<String, Object> column = HFactory.createColumn(key, value, NAME_SERIALIZER, VALUE_SERIALIZER);
            mutator.addInsertion(id, columnFamily, column);
        }
        for (String key : keysToRemove) {
            mutator.addDeletion(id, columnFamily, key, StringSerializer.get());
        }
        mutator.execute();
    }

    public boolean load(long id, Registry registry) {
        SliceQuery<Long, String, Object> query = HFactory.createSliceQuery(keyspace,
                KEY_SERIALIZER, NAME_SERIALIZER, VALUE_SERIALIZER);

        query.setKey(id);
        query.setColumnFamily(columnFamily);
        query.setRange(null, null, false, Integer.MAX_VALUE);

        QueryResult<ColumnSlice<String,Object>> queryResult = query.execute();
        ColumnSlice<String, Object> slice = queryResult.get();

        Map<String, Object> map = new TreeMap<String, Object>();
        for (HColumn<String, Object> column : slice.getColumns()) {
            map.put(column.getName(), column.getValue());
        }
        Registries.putMassivly(registry, map);
        return true;
    }

    public ColumnFamilyDefinition columnDef() {
        return HFactory.createColumnFamilyDefinition(
                keyspace.getKeyspaceName(),
                columnFamily,
                ComparatorType.UTF8TYPE);
    }

    public IdRangeIterator getIdRangeSelector(final Long start, final Long end) {
        return new SimpleIdRangeIterator(start, end);
    }

    public static class FilterKeyIterator<K> implements Iterable<K> {
        private static StringSerializer stringSerializer = new StringSerializer();

        private static int MAX_ROW_COUNT_DEFAULT = 500;
        private int maxColumnCount = 2;	// we only need this to tell if there are any columns in the row (to test for tombstones)

        private Iterator<Row<K, String, String>> rowsIterator = null;

        private RangeSlicesQuery<K, String, String> query = null;

        private K nextValue = null;
        private K lastReadValue = null;
        private K endKey;
        private boolean firstRun = true;

        private Iterator<K> keyIterator = new Iterator<K>() {
            public boolean hasNext() {
                return nextValue != null;
            }

            public K next() {
                K next = nextValue;
                findNext(false);
                return next;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

        private void findNext(boolean fromRunQuery) {
            nextValue = null;
            if (rowsIterator == null) {
                return;
            }
            while (rowsIterator.hasNext() && nextValue == null) {
                Row<K, String, String> row = rowsIterator.next();
                lastReadValue = row.getKey();
                if (!row.getColumnSlice().getColumns().isEmpty()) {
                    nextValue = lastReadValue;
                }
            }
            if (!rowsIterator.hasNext() && nextValue == null) {
                runQuery(lastReadValue, endKey);
            }
        }

        public FilterKeyIterator(Keyspace keyspace, String columnFamily, Serializer<K> serializer) {
            this(keyspace, columnFamily, serializer, null, null, MAX_ROW_COUNT_DEFAULT);
        }

        public FilterKeyIterator(Keyspace keyspace, String columnFamily, Serializer<K> serializer, int maxRowCount) {
            this(keyspace, columnFamily, serializer, null, null, maxRowCount);
        }

        public FilterKeyIterator(Keyspace keyspace, String columnFamily, Serializer<K> serializer, K start, K end) {
            this(keyspace, columnFamily, serializer, start, end, MAX_ROW_COUNT_DEFAULT);
        }

        public FilterKeyIterator(Keyspace keyspace, String columnFamily, Serializer<K> serializer, K start, K end, String []filter) {
            this(keyspace, columnFamily, serializer, start, end, filter, MAX_ROW_COUNT_DEFAULT);
        }

        public FilterKeyIterator(Keyspace keyspace, String columnFamily, Serializer<K> serializer, K start, K end, int maxRowCount) {
            query = HFactory
                    .createRangeSlicesQuery(keyspace, serializer, stringSerializer, stringSerializer)
                    .setColumnFamily(columnFamily)
                    .setRange(null, null, false, maxColumnCount)
                    .setRowCount(maxRowCount);

            endKey = end;
            runQuery(start, end);
        }

        public FilterKeyIterator(Keyspace keyspace, String columnFamily, Serializer<K> serializer, K start, K end, String []filter, int maxRowCount) {
            query = HFactory
                    .createRangeSlicesQuery(keyspace, serializer, stringSerializer, stringSerializer)
                    .setColumnFamily(columnFamily)
                    .setColumnNames(filter)
                    .setRowCount(maxRowCount);

            endKey = end;
            runQuery(start, end);
        }

        private void runQuery(K start, K end) {
            query.setKeys(start, end);

            rowsIterator = null;
            QueryResult<OrderedRows<K, String, String>> result = query.execute();
            OrderedRows<K, String, String> rows = (result != null) ? result.get() : null;
            rowsIterator = (rows != null) ? rows.iterator() : null;

            // we'll skip this first one, since it is the same as the last one from previous time we executed
            if (!firstRun  && rowsIterator != null)
                rowsIterator.next();

            firstRun = false;

            if (!rowsIterator.hasNext()) {
                nextValue = null;    // all done.  our iterator's hasNext() will now return false;
            } else {
                findNext(true);
            }
        }

        public Iterator<K> iterator() {
            return keyIterator;
        }
    }



    private class CassandraBeanStorer implements BeanStorer {
        private Long id;
        private final Registry registry;
        private List<String> keysToRemove = new ArrayList<String>();

        public CassandraBeanStorer(Registry registry) {
            this.registry = registry;
        }

        public BeanStorer renewId() {
            id = newId();
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
            return CassandraStore.this.load(id, registry);
        }

        public BeanStorer store() {
            if (id == null) {
                renewId();
            }
            CassandraStore.this.store(id, registry, keysToRemove);
            keysToRemove.clear();
            return this;
        }

        public void dispose() {
        }

        public Registry getRegistry() {
            return registry;
        }

        public <T extends RegistryBean> T getBean(Class<T> clazz) {
            return registry.bean(clazz);
        }

        public BeanStorer subStorer(String... path) {
            throw new UnsupportedOperationException("subStorer");
        }

        public void addRemovedKey(String key) {
            keysToRemove.add(key);
        }
    }

    private class SimpleIdRangeIterator implements IdRangeIterator {
        private Iterator<Long> iterator;
        private List<String> filters;
        private Long peekedValue;
        private final Long start;
        private final Long end;

        public SimpleIdRangeIterator(Long start, Long end) {
            this.start = start;
            this.end = end;
            filters = new ArrayList<String>();
        }

        public Long peek() {
            if (peekedValue != null) {
                return peekedValue;
            }
            init();
            while (iterator.hasNext()) {
                peekedValue = iterator.next();
                return peekedValue;
            }
            return null;
        }

        public Long next() {
            Long value = peek();
            peekedValue = null;
            return value;
        }

        public boolean hasNext() {
            return peek() != null;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        private void init() {
            if (iterator != null) {
                return;
            }
            FilterKeyIterator<Long> keyIterator = new FilterKeyIterator<Long>(keyspace,
                    columnFamily,
                    LongSerializer.get(),
                    start,
                    end,
                    filters.toArray(new String[filters.size()]));
            iterator = keyIterator.iterator();
        }

        public IdRangeIterator addFilter(String... pathes) {
            filters.addAll(Arrays.asList(pathes));
            return this;
        }
    }
}

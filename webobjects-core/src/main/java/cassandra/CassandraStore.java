package cassandra;

import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.ObjectSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.template.ColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.ColumnFamilyUpdater;
import me.prettyprint.cassandra.service.template.ThriftColumnFamilyTemplate;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;
import registry.Registries;
import registry.Registry;
import store.IdGenerator;
import store.RegistryStore;

import java.util.Map;
import java.util.TreeMap;

/**
 * User: cap_protect
 * Date: 5/8/12
 * Time: 10:45 AM
 */
public class CassandraStore implements RegistryStore {
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

    public void store(long id, Registry registry) {
        Mutator<Long> mutator = template.createMutator();
        Map<String, Object> properties = Registries.liniarize(registry);
        for (String key : properties.keySet()) {
            Object value = properties.get(key);
            HColumn<String, Object> column = HFactory.createColumn(key, value, NAME_SERIALIZER, VALUE_SERIALIZER);
            mutator.addInsertion(id, columnFamily, column);
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

    public static ColumnFamilyDefinition columnDef(String keyspace, String columnFamily) {
        return HFactory.createColumnFamilyDefinition(keyspace, columnFamily, ComparatorType.UTF8TYPE);
    }
}

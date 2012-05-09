package casasndra;

import cassandra.CassandraStore;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import store.IdGenerator;
import store.RegistryStore;

import java.util.Arrays;
import java.util.List;

/**
 * User: cap_protect
 * Date: 5/9/12
 * Time: 7:54 AM
 */
public class CassandraCase {
    public static final String TEST_KEYSPACE = "testks";
    public static final String TEST_COLUMN_FAMILY = "store";

    public static void init(Cluster cluster) {

        String strategy = "org.apache.cassandra.locator.SimpleStrategy";

        List<ColumnFamilyDefinition> cfDefs = Arrays.asList(CassandraStore.columnDef(TEST_KEYSPACE, TEST_COLUMN_FAMILY));
        if (cluster.describeKeyspace(TEST_KEYSPACE) != null) {
            cluster.dropKeyspace(TEST_KEYSPACE);
        }
        cluster.addKeyspace(HFactory.createKeyspaceDefinition(TEST_KEYSPACE, strategy, 1, cfDefs));

    }

    public static RegistryStore getStore(Cluster cluster) {
        IdGenerator gen = IdGenerator.getGenerator(IdGenerator.Type.SEQUENTIAL);
        Keyspace keyspace = HFactory.createKeyspace(TEST_KEYSPACE, cluster);
        return new CassandraStore(gen, keyspace, TEST_COLUMN_FAMILY);
    }

    public static void cleanup(Cluster cluster) {
        cluster.dropKeyspace(TEST_KEYSPACE);
    }
}

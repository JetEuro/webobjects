package org.webobjects;

import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import org.webobjects.WebObjectsFactory;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import org.webobjects.WebObjectsTestCase;

/**
 * User: cap_protect
 * Date: 5/9/12
 * Time: 7:54 AM
 */
public class CassandraTestCase extends WebObjectsTestCase {
    public static final String TEST_KEYSPACE = "testks";
    public static final String TEST_COLUMN_FAMILY = "org.webobjects.store";

    private Cluster cluster;
    private WebObjectsFactory.Cassandra factory;

    public CassandraTestCase(Cluster cluster) {
        this.cluster = cluster;
        Keyspace keyspace = HFactory.createKeyspace(TEST_KEYSPACE, cluster);
        factory = WebObjectsFactory.cassandra(keyspace);
    }

    public void init() {

        String strategy = "org.apache.org.webobjects.cassandra.locator.SimpleStrategy";

        if (cluster.describeKeyspace(TEST_KEYSPACE) != null) {
            cluster.dropKeyspace(TEST_KEYSPACE);
        }
        KeyspaceDefinition ksDef = HFactory.createKeyspaceDefinition(
                TEST_KEYSPACE, strategy, 1, factory.getColumnDefinitions());
        cluster.addKeyspace(ksDef, true);
    }

    public void cleanup() {
        cluster.dropKeyspace(TEST_KEYSPACE);
    }

    public WebObjectsFactory getFactory() {
        return factory;
    }
}

package org.webobjects;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * User: cap_protect
 * Date: 5/10/12
 * Time: 8:47 AM
 */
public abstract class WebObjectsTestCase {
    public static final String CASSANDRA_TEST_KEYSPACE = "test";
    public static final String CASSANDRA_TEST_COLUMN_FAMILY = "store";

    public abstract void init();

    public abstract WebObjectsFactory getFactory();

    public abstract void cleanup();

    public static WebObjectsTestCase cassandra(final Cluster cluster) {
        return new CassandraTestCase(true, new ClusterFactory() {
            public Cluster getCluster() {
                return cluster;
            }
        });
    }

    public static WebObjectsTestCase cassandra(boolean persistent, ClusterFactory factory) {
        return new CassandraTestCase(persistent, factory);
    }

    public static WebObjectsTestCase cassandra(ClusterFactory factory) {
        return new CassandraTestCase(false, factory);
    }

    public static WebObjectsTestCase inMemory() {
        return new InMemoryTestCase();
    }

    private static class CassandraTestCase extends WebObjectsTestCase {

        private Cluster cluster;
        private final boolean persistent;
        private WebObjectsFactory factory;
        private List<ColumnFamilyDefinition> columnDefinitions = new ArrayList<ColumnFamilyDefinition>();
        private ClusterFactory clusterFactory;

        public CassandraTestCase(boolean persistent, ClusterFactory factory) {
            this.clusterFactory = factory;
            this.persistent = persistent;
        }

        public CassandraTestCase setCluster(Cluster cluster) {
            if (!persistent) {
                throw new IllegalArgumentException("error");
            }
            this.cluster = cluster;
            return this;
        }

        public void init() {
            renewCluster();

            String strategy = "org.apache.cassandra.locator.SimpleStrategy";

            if (cluster.describeKeyspace(CASSANDRA_TEST_KEYSPACE) != null) {
                cluster.dropKeyspace(CASSANDRA_TEST_KEYSPACE);
            }
            List<ColumnFamilyDefinition> defs = new ArrayList<ColumnFamilyDefinition>(columnDefinitions);
            defs.addAll(factory.getColumnDefinitions());
            KeyspaceDefinition ksDef = HFactory.createKeyspaceDefinition(
                    CASSANDRA_TEST_KEYSPACE, strategy, 1, defs);
            cluster.addKeyspace(ksDef, true);
        }

        private void renewCluster() {
            if (persistent) {
                if (cluster == null) {
                    cluster = clusterFactory.getCluster();
                }
            } else {
                cluster = clusterFactory.getCluster();
            }
        }

        public void cleanup() {
            renewCluster();
            cluster.dropKeyspace(CASSANDRA_TEST_KEYSPACE);
        }

        public WebObjectsFactory getFactory() {
            renewCluster();
            if (factory == null || !persistent) {
                sinkDefinitions();
                Keyspace keyspace = HFactory.createKeyspace(CASSANDRA_TEST_KEYSPACE, cluster);
                factory = WebObjectsFactory.cassandra(keyspace);
            }
            return factory;
        }

        private void sinkDefinitions() {
            if (factory != null) {
                columnDefinitions.addAll(factory.getColumnDefinitions());
            }
        }
    }

    private static class InMemoryTestCase extends WebObjectsTestCase {
        private WebObjectsFactory factory = WebObjectsFactory.inMemory();

        @Override
        public void init() {
        }

        @Override
        public WebObjectsFactory getFactory() {
            return factory;
        }

        @Override
        public void cleanup() {
        }
    }

    public interface ClusterFactory {
        Cluster getCluster();
    }
}
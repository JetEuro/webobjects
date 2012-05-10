package org.webobjects.casasndra;

import org.webobjects.CassandraTestCase;
import org.webobjects.beans.ImageTree;
import org.webobjects.beans.ImageTreeNode;
import org.webobjects.beans.User;
import org.webobjects.cassandra.CassandraStore;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.factory.HFactory;
import org.junit.runner.RunWith;
import org.webobjects.registry.Registries;
import org.webobjects.store.RegistryStore;
import org.webobjects.store.RegistryStoreUtils;

/**
 * User: cap_protect
 * Date: 5/8/12
 * Time: 9:20 AM
 */
@RunWith(JDaveRunner.class)
public class CassandraStoreTest extends Specification<CassandraStore> {

    public class StoreAndLoadUserLocal {
        public static final int BEAN_ID = 0;
        private Cluster cluster = HFactory.getOrCreateCluster("Test Cluster", "localhost");
        private CassandraTestCase testCase = new CassandraTestCase(cluster);
        private RegistryStore store = testCase.getFactory()
                .store()
                .setName(CassandraTestCase.TEST_COLUMN_FAMILY)
                .create();

        public void init() {
            testCase.init();
        }

        public void store() {
            User userBean = Registries.newBean(User.class);

            ImageTree tree = userBean.getTree();
            ImageTreeNode root = tree.getRoot();

            root.get(0).getImage().setSrc("img0");
            root.get(1).getImage().setSrc("img1");
            root.get(2).getImage().setSrc("img2");
            root.get(0).get(0).getImage().setSrc("img00");
            root.get(0).get(1).getImage().setSrc("img01");
            root.get(0).get(2).getImage().setSrc("img02");

            RegistryStoreUtils.write(store, BEAN_ID, userBean);
        }

        public void load() {
            User user = RegistryStoreUtils.read(store, 0, User.class);
            ImageTree tree = user.getTree();
            ImageTreeNode root = tree.getRoot();

            specify(user.getTree().getRoot().size(), 3);
            specify(user.getTree().getRoot().get(0).size(), 3);

            specify(root.get(0).getImage().getSrc(), "img0");
            specify(root.get(1).getImage().getSrc(), "img1");
            specify(root.get(2).getImage().getSrc(), "img2");
            specify(root.get(0).get(0).getImage().getSrc(), "img00");
            specify(root.get(0).get(1).getImage().getSrc(), "img01");
            specify(root.get(0).get(2).getImage().getSrc(), "img02");
        }

        public void cleanup() {
            testCase.cleanup();
        }
    }

}

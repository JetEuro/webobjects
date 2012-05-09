package casasndra;

import beans.ImageTree;
import beans.ImageTreeNode;
import beans.User;
import cassandra.CassandraStore;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.factory.HFactory;
import org.junit.runner.RunWith;
import registry.Registries;
import store.RegistryStore;
import store.RegistryStoreUtils;

/**
 * User: cap_protect
 * Date: 5/8/12
 * Time: 9:20 AM
 */
@RunWith(JDaveRunner.class)
public class CassandraStoreTest extends Specification<CassandraStore> {

    public class StoreAndLoadUserLocal {
        public static final int BEAN_ID = 0;
        private Cluster cluster = HFactory.getOrCreateCluster("cluster", "localhost");
        private RegistryStore store;

        public void init() {
            CassandraCase.init(cluster);
        }

        public void store() {
            store = CassandraCase.getStore(cluster);
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
            store = CassandraCase.getStore(cluster);
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
            CassandraCase.cleanup(cluster);
        }
    }

}

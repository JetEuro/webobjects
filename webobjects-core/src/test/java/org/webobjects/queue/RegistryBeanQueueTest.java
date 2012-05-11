package org.webobjects.queue;

import jdave.Specification;
import jdave.junit4.JDaveRunner;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.factory.HFactory;
import org.junit.runner.RunWith;
import org.webobjects.WebObjectsTestCase;
import org.webobjects.registry.Polymorphic;
import org.webobjects.registry.Registries;
import org.webobjects.registry.RegistryBean;

import java.util.HashSet;
import java.util.Queue;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 1:01 PM
 */
@RunWith(JDaveRunner.class)
public class RegistryBeanQueueTest extends Specification<RegistryBeanQueue> {
    public interface QueueElement extends RegistryBean, Polymorphic {
        String getValue();

        void setValue(String value);
    }

    public interface QueueElementSubType1 extends QueueElement {
        void setField1(String element);

        String getField1();
    }

    public interface QueueElementSubType2 extends QueueElement {
        void setField2(String element);

        String getField2();
    }


    public abstract class TakeOfferElement {
        protected abstract WebObjectsTestCase getCase();

        private WebObjectsTestCase testCase = getCase();

        private RegistryBeanQueue<QueueElement> queue = testCase
                .getFactory()
                .<QueueElement>registryBeanQueue()
                .setStoreName(WebObjectsTestCase.CASSANDRA_TEST_COLUMN_FAMILY)
                .create();

        public void takeOffer() {
            testCase.init();

            QueueElementSubType1 element1 = Registries.newBean(QueueElementSubType1.class);
            element1.setValue("el1");
            element1.setField1("f1v");
            queue.add(element1);

            QueueElementSubType2 element2 = Registries.newBean(QueueElementSubType2.class);
            element2.setValue("el2");
            element2.setField2("f2v");
            queue.add(element2);

            QueueElement element3 = queue.remove();
            QueueElement element4 = queue.remove();

            HashSet set1 = new HashSet();
            set1.add(element1);
            set1.add(element2);

            HashSet set2 = new HashSet();
            set2.add(element3);
            set2.add(element4);

            specify(set2, should.equal(set1));

            testCase.cleanup();
        }
    }

    public class InMemoryTakeOfferElement extends TakeOfferElement {
        @Override
        protected WebObjectsTestCase getCase() {
            return WebObjectsTestCase.inMemory();
        }
    }

    public class CassandraTakeOfferElement extends TakeOfferElement {
        @Override
        protected WebObjectsTestCase getCase() {
            Cluster cluster = HFactory.getOrCreateCluster("Test Cluster", "localhost");
            return WebObjectsTestCase.cassandra(cluster);
        }
    }

}

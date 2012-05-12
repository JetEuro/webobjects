package org.webobjects.service;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.factory.HFactory;
import org.junit.Ignore;
import org.webobjects.WebObjectsFactory;
import org.webobjects.WebObjectsTestCase;
import org.webobjects.beans.AddCommentRequest;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.junit.runner.RunWith;
import org.webobjects.registry.*;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 9:36 PM
 */
@RunWith(JDaveRunner.class)
public class RegistryExecutorTest extends Specification<RegistryExecutor> {

    public abstract class ExecuteTask {
        private WebObjectsTestCase testCase = getCase();

        public abstract WebObjectsTestCase getCase();

        private RegistryExecutor executor = testCase.getFactory().executor().create();

        private final Object lock = new Object();
        private int value;


        public void postPone() {
            testCase.init();
            executor.setRunner(new TaskRunner());
            executor.start();

            AddCommentRequest task = Registries.newBean(AddCommentRequest.class);
            task.getUser().setAge(25);
            executor.execute(task);

            awaitTillValueEquals(25, 5000);
            specify(value, 25);

            testCase.cleanup();
        }

        private void awaitTillValueEquals(int value, int timeout) {
            long time = System.currentTimeMillis();
            long past = System.currentTimeMillis() - time;
            synchronized (lock) {
                try {
                    while (past < timeout && this.value != value) {
                        lock.wait(timeout - past);
                        past = System.currentTimeMillis() - time;
                    }
                } catch (InterruptedException e) {
                    // skip
                }
            }
        }

        private class TaskRunner implements RegistryDelegate<AddCommentRequest> {
            public Object execute(AddCommentRequest self, Object... args) {
                synchronized (lock) {
                    value = self.getUser().getAge();
                    lock.notify();
                }
                return null;
            }
        }
    }

    public class CassandraExecuteTask extends ExecuteTask {
        @Override
        public WebObjectsTestCase getCase() {
            Cluster cluster = HFactory.getOrCreateCluster("Task Cluster", "localhost");
            return WebObjectsTestCase.cassandra(cluster);
        }
    }

    public class InMemoryExecuteTask extends ExecuteTask {
        @Override
        public WebObjectsTestCase getCase() {
            return WebObjectsTestCase.inMemory();
        }
    }
}

package org.webobjects.distributed;

import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.junit.runner.RunWith;
import org.webobjects.store.IdGenerator;
import org.webobjects.store.RegistryStore;
import org.webobjects.store.RegistryStores;

/**
 * User: cap_protect
 * Date: 5/9/12
 * Time: 10:29 AM
 */
@RunWith(JDaveRunner.class)
public class ClusterStateMonitorTest extends Specification<ClusterStateMonitor> {
    public class TwoStateMonitors {
        private RegistryStore store = RegistryStores.inMemory(IdGenerator.Type.SEQUENTIAL);

        private ClusterStateMonitor monitor1 = new ClusterStateMonitor(store);
        private ClusterStateMonitor monitor2 = new ClusterStateMonitor(store);

        public void statesInterchangable() {
            monitor1.start();
            monitor2.start();

            monitor1.ping();
            monitor2.ping();

            monitor1.check();
            monitor2.check();

            specify(monitor1.getState().getStates().size(), 2);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                // skip
            }
            specify(monitor2.getState().getStates().size(), 2);

            monitor1.stop();
            monitor2.stop();
        }
    }
}

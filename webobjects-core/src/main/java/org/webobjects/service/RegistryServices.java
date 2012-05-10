package org.webobjects.service;

import org.webobjects.registry.RegistryBean;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 9:36 PM
 */
public class RegistryServices {
    public static RegistryService localRegistryService(final int nThreads) {
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        return executorRegistryService(executor);
    }

    public static RegistryService executorRegistryService(final Executor executor) {
        return new RegistryService() {
            public <T extends RegistryBean> void submit(final RegistryTask<T> task) {
                executor.execute(new Runnable() {
                    public void run() {
                        task.run();
                    }
                });
            }
        };
    }

    public static RegistryService runRegistryService() {
        return new RegistryService() {
            public <T extends RegistryBean> void submit(final RegistryTask<T> task) {
                task.run();
            }
        };
    }
}

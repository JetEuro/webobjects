package org.webobjects.service;

import org.webobjects.queue.RegistryBeanQueue;
import org.webobjects.registry.ExecutionContext;
import org.webobjects.registry.RegistryDelegate;
import org.webobjects.registry.RegistryGettable;

/**
 * User: cap_protect
 * Date: 5/12/12
 * Time: 7:28 AM
 */
public class RegistryQueueExecutor implements RegistryExecutor, Runnable {
    private final RegistryBeanQueue<RegistryTask> queue;
    private final ThreadGroup threadGroup;
    private final int nThreads;
    private final ExecutionContext context = new ExecutionContext();
    private Thread [] threadArray;

    public RegistryQueueExecutor(RegistryBeanQueue<RegistryTask> queue, ThreadGroup group, int nThreads) {
        this.queue = queue;
        threadGroup = group;
        this.nThreads = nThreads;
    }

    public void start() {
        if (threadArray != null) {
            throw new IllegalStateException("RegistryQueueExector bad state");
        }
        threadArray = new Thread[nThreads];
        for (int i = 0; i < nThreads; i++) {
            Thread thread = new Thread(threadGroup, this);
            thread.setName(threadGroup.getName() + " #" + i);
            threadArray[i] = thread;
        }
        for (Thread thread : threadArray) {
            thread.start();
        }
    }

    public void stop() {
        if (threadArray == null) {
            throw new IllegalStateException("RegistryQueueExector bad state");
        }

        for (Thread thread : threadArray) {
            thread.interrupt();
        }
        for (Thread thread : threadArray) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        threadArray = null;
    }


    public ExecutionContext getExecutionContext() {
        return context;
    }

    public void setRunner(RegistryDelegate<? extends RegistryTask> delegate) {
        context.bind("run()", delegate);
    }

    public void execute(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable");
        }
        if (runnable instanceof RegistryTask) {
            queue.add((RegistryTask) runnable);
            return;
        }
        throw new IllegalArgumentException("runnable should implement RegistryTask");
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                RegistryTask task = queue.take();
                task.getRegistry().setExecutionContext(context);
                task.run();
            } catch (InterruptedException ex) {
                break;
            } catch (Error ex) {
                break;
            } catch (Throwable thr) {
                continue;
            }
        }
    }
}

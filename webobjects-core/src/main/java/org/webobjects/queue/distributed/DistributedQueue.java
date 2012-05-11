package org.webobjects.queue.distributed;

import org.webobjects.store.RegistryStore;

import java.util.concurrent.BlockingQueue;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 11:55 AM
 */
public interface DistributedQueue<T> extends BlockingQueue<T> {
}

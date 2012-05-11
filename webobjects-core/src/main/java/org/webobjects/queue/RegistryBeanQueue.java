package org.webobjects.queue;

import org.webobjects.queue.distributed.DistributedQueue;
import org.webobjects.registry.RegistryBean;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 11:55 AM
 */
public interface RegistryBeanQueue<T extends RegistryBean> extends DistributedQueue<T> {
}

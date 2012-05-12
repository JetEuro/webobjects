package org.webobjects.service;

import org.webobjects.registry.ExecutionContext;
import org.webobjects.registry.RegistryBean;
import org.webobjects.registry.RegistryDelegate;

import java.util.concurrent.Executor;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 9:33 PM
 */
public interface RegistryExecutor extends Executor {
    ExecutionContext getExecutionContext();

    void setRunner(RegistryDelegate<? extends RegistryTask> delegate);

    void start();

    void stop();
}

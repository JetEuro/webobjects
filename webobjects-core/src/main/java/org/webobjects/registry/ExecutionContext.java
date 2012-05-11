package org.webobjects.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 6:12 PM
 */
public class ExecutionContext {
    private final Map<String, RegistryDelegate> taskDelegateMap = Collections.synchronizedMap(new HashMap());

    public ExecutionContext bind(String taskName, RegistryDelegate delegate) {
        taskDelegateMap.put(taskName, delegate);
        return this;
    }

    public RegistryDelegate selectDelegate(RegistryHandler taskName) {
        String[] names = taskName.value().trim().split("\\s*,\\s*");
        for (String name : names) {
            RegistryDelegate delegate = taskDelegateMap.get(name);
            if (delegate != null) {
                return delegate;
            }
        }
        return null;
    }
}

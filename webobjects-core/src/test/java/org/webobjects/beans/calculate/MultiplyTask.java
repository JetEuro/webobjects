package org.webobjects.beans.calculate;

import org.webobjects.registry.RegistryHandler;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 6:33 PM
 */
public interface MultiplyTask extends CalculateTask {
    @RegistryHandler("multiply")
    void run();
}

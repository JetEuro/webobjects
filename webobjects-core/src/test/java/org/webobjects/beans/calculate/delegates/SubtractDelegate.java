package org.webobjects.beans.calculate.delegates;

import org.webobjects.beans.calculate.SubtractTask;
import org.webobjects.registry.RegistryDelegate;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 6:35 PM
 */
public class SubtractDelegate implements RegistryDelegate<SubtractTask> {
    public Object execute(SubtractTask self, Object... args) {
        self.setC(self.getA() - self.getB());
        return null;
    }
}

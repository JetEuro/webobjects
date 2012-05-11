package org.webobjects.beans.calculate.delegates;

import org.webobjects.beans.calculate.AddTask;
import org.webobjects.beans.calculate.SubtractTask;
import org.webobjects.registry.RegistryDelegate;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 6:35 PM
 */
public class AddDelegate implements RegistryDelegate<AddTask> {
    public Object execute(AddTask self, Object... args) {
        self.setC(self.getA() + self.getB());
        return null;
    }
}

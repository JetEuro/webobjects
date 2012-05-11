package org.webobjects.beans.calculate;

import org.webobjects.beans.Task;
import org.webobjects.registry.Polymorphic;
import org.webobjects.registry.RegistryGettable;
import org.webobjects.store.Storable;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 6:33 PM
 */
public interface CalculateTask extends Task, Polymorphic, Storable, RegistryGettable {
    void setA(int a);

    int getA();

    void setB(int b);

    int getB();

    void setC(int c);

    int getC();
}

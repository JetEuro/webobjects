package org.webobjects;

import org.webobjects.WebObjectsFactory;

/**
 * User: TCSDEVELOPER
 * Date: 5/10/12
 * Time: 8:47 AM
 */
public abstract class WebObjectsTestCase {
    public abstract void init();

    public abstract WebObjectsFactory getFactory();

    public abstract void cleanup();
}

package org.webobjects;

/**
 * User: TCSDEVELOPER
 * Date: 5/10/12
 * Time: 8:49 AM
 */
public class InMemoryTestCase extends WebObjectsTestCase {
    private WebObjectsFactory.InMemory inMemory = WebObjectsFactory.inMemory();

    @Override
    public void init() {
    }

    @Override
    public WebObjectsFactory getFactory() {
        return inMemory;
    }

    @Override
    public void cleanup() {
    }
}

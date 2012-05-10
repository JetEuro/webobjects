package org.webobjects.store;

/**
 * User: TCSDEVELOPER
 * Date: 5/9/12
 * Time: 1:18 PM
 */
public interface IdRangeIterator {
    long[] next(int count);
}

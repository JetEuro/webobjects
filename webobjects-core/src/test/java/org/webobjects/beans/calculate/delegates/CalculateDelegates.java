package org.webobjects.beans.calculate.delegates;

import org.webobjects.registry.ExecutionContext;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 6:36 PM
 */
public class CalculateDelegates {
    public static final String ADD = "add";
    public static final String SUBTRACT = "subtract";
    public static final String MULTIPLY = "multiply";
    public static final String DIVIDE = "divide";

    public static void defaultContext(ExecutionContext ctx) {
        ctx
                .bind(ADD, new AddDelegate())
                .bind(SUBTRACT, new SubtractDelegate())
                .bind(MULTIPLY, new MultiplyDelegate())
                .bind(DIVIDE, new DivideDelegate());
    }

}

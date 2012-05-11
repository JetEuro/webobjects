package org.webobjects.beans.delegates;

import org.webobjects.beans.AddCommentRequest;
import org.webobjects.registry.RegistryDelegate;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 5:54 PM
 */
public class RunRequestProcessDelegate implements RegistryDelegate<AddCommentRequest> {
    public Object execute(AddCommentRequest self, Object... args) {
        StringBuilder builder = (StringBuilder) args[0];

        builder.append(self.getUser().getCredentials().getUsername());
        builder.append(": ");
        builder.append(self.getText());
        return null;
    }
}

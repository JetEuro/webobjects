package org.webobjects.sample.forum.app.validators;

import org.webobjects.sample.forum.app.beans.User;
import org.webobjects.web.SimpleBeanValidator;

import javax.servlet.http.HttpServletRequest;

/**
* User: TCSDEVELOPER
* Date: 5/15/12
* Time: 10:10 AM
*/
public class UserBeanValidator extends SimpleBeanValidator<User> {
    public UserBeanValidator() {
        super(User.class);
    }

    public void loadParameters(HttpServletRequest request) {
        load(request, "username", "email", "password");
        loadOther(request, "confirmation");
    }

    public void loadDefaults() {
        beanRegistry.put("username", "");
        beanRegistry.put("email", "");
        beanRegistry.put("password", "");
        otherValues.put("confirmation", "");
    }

    public boolean validate() {
        checkExists("field '%1s' do not exists",
                "username", "email", "password", "confirmation");

        checkNotEmpty("field '%1s' should not be empty",
                "username", "email", "password", "confirmation");

        checkEquals("field 'password' should be equal to field 'confirmation'",
                "password", "confirmation");

        return validationResult;
    }

}

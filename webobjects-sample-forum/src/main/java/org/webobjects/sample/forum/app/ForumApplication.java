package org.webobjects.sample.forum.app;

import org.webobjects.WebObjectsFactory;
import org.webobjects.registry.Registries;
import org.webobjects.sample.forum.app.beans.User;
import org.webobjects.sample.forum.app.tasks.AddUserTask;
import org.webobjects.sample.forum.app.validators.UserBeanValidator;
import org.webobjects.service.RegistryTask;
import org.webobjects.store.RegistryStore;
import org.webobjects.store.RegistryStoreUtils;
import org.webobjects.web.BeanValidator;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;

/**
 * User: cap_protect
 * Date: 5/13/12
 * Time: 12:39 PM
 */
public class ForumApplication {

    @WebListener
    public static class ForumApplicationInitializer implements ServletContextListener {

        public void contextInitialized(ServletContextEvent servletContextEvent) {
            ForumApplication app = new ForumApplication();
            app.init();
            ServletContext context = servletContextEvent.getServletContext();
            context.setAttribute(ForumApplication.class.getName(), app);
        }

        public void contextDestroyed(ServletContextEvent servletContextEvent) {

        }
    }
    private RegistryStore userStore;
    private WebObjectsFactory factory;

    public ForumApplication() {

    }

    public WebObjectsFactory getFactory() {
        return factory;
    }

    public void setFactory(WebObjectsFactory factory) {
        this.factory = factory;
    }

    public void init() {
        if (factory == null) {
            factory = WebObjectsFactory.inMemory();
        }

        userStore = factory.store().setName("Users").create();
    }

    public RegistryStore getUserStore() {
        return userStore;
    }

    public BeanValidator<User> createUserValidator() {

        return new UserBeanValidator();
    }

    public long addUser(BeanValidator<User> validator) {
        RegistryStore store = getUserStore();
        User user = validator.bean();
        long id = RegistryStoreUtils.write(store, user);

        AddUserTask addUserTask = Registries.newBean(AddUserTask.class);
        addUserTask.setUserId(id);
        submit(addUserTask);
        return id;
    }

    public void submit(RegistryTask task) {
        System.out.println(task);
    }

    public static ForumApplication get(HttpServletRequest request) {
        return get(request.getServletContext());
    }

    public static ForumApplication get(ServletContext servletContext) {
        return (ForumApplication) servletContext.getAttribute(ForumApplication.class.getName());
    }
}

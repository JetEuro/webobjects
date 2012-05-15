package org.webobjects.sample.forum.servlets;

import org.webobjects.sample.forum.app.ForumApplication;
import org.webobjects.sample.forum.app.beans.User;
import org.webobjects.web.BeanValidator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User: cap_protect
 * Date: 5/13/12
 * Time: 12:51 PM
 */
@WebServlet(name = "RegisterServlet", urlPatterns = {RegisterServlet.REGISTER_URL}, loadOnStartup = 1)
public class RegisterServlet extends HttpServlet {
    private static final String REGISTER_JSP_PATH = "/WEB-INF/register.jsp";
    private static final String REGISTER_BUTTON_PARAMETER = "registerButton";
    public static final String REGISTER_URL = "/register";

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        System.out.println("hello world!");
        super.init(servletConfig);    //To change body of overridden methods use File | Settings | File Templates.
    }

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        if (request.getParameter(REGISTER_BUTTON_PARAMETER) == null) {
            return;
        }

        ForumApplication application = ForumApplication.get(request);
        BeanValidator<User> validator = application.createUserValidator();

        validator.loadParameters(request);

        if (!validator.validate()) {
            forwardRegisterJsp(request, response, validator);
            return;
        }

        response.getWriter().println("OK " + application.addUser(validator));
    }

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        ForumApplication application = ForumApplication.get(request);

        BeanValidator<User> validator = application.createUserValidator();
        validator.loadDefaults();
        forwardRegisterJsp(request, response, validator);
    }

    private void forwardRegisterJsp(HttpServletRequest request,
                                    HttpServletResponse response,
                                    BeanValidator<User> validator) throws ServletException, IOException {
        request.setAttribute("validator", validator);
        request.setAttribute("user", validator.bean());
        request.setAttribute("message", validator.getMessages());
        request.setAttribute("otherValues", validator.getOtherValues());

        getServletContext()
                .getRequestDispatcher(REGISTER_JSP_PATH)
                .forward(request, response);
    }
}

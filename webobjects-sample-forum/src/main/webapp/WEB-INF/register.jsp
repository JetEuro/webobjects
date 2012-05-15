<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<jsp:useBean id="user" scope="request" type="org.webobjects.sample.forum.app.beans.User" />
<jsp:useBean id="validator" scope="request" type="org.webobjects.web.BeanValidator" />
<jsp:useBean id="otherValues" scope="request" type="org.webobjects.registry.RegistryBean" />
<html>
<head><title>Register user</title></head>
<body>
<div class="form">
    <h3>Register user</h3>
    <form method="POST">
        <label for="email">e-mail:</label>
        <input id="email" name="email" value="${user.email}"/>
        <div class="error">
            ${validator.getMessage('email')}
        </div>

        <label for="username">username:</label>
        <input id="username" name="username" value="${user.username}"/>
        <div class="error">
            ${validator.getMessage('username')}
        </div>

        <label for="password">password:</label>
        <input id="password" type="password" name="password"  value="${user.password}"/>
        <div class="error">
            ${validator.getMessage('password')}
        </div>

        <label for="confirmation">confirmation:</label>
        <input id="confirmation" type="password" name="confirmation"  value="${otherValues['confirmation']}" />
        <div class="error">
            ${validator.getMessage('confirmation')}
        </div>

        <input name="registerButton" type="submit" value="Register!">
    </form>
</div>
</body>
</html>
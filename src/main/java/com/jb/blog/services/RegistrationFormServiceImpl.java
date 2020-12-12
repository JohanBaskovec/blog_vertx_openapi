package com.jb.blog.services;

import org.openapitools.vertxweb.server.model.RegistrationForm;
import org.openapitools.vertxweb.server.model.User;

public class RegistrationFormServiceImpl implements RegistrationFormService {
    @Override
    public User toUser(RegistrationForm registrationForm) {
        return new User(registrationForm.getUsername(), registrationForm.getPassword(), 0);
    }
}

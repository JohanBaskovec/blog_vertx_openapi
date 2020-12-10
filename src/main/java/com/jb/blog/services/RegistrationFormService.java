package com.jb.blog.services;

import org.openapitools.vertxweb.server.model.RegistrationForm;
import org.openapitools.vertxweb.server.model.User;

public interface RegistrationFormService {
    User toUser(RegistrationForm registrationForm);
}

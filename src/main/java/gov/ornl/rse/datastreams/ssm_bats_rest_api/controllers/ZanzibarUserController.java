package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization.AuthorizationHandler;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;

/**
 * Controller for endpoints administrating Zanzibar users.
 *
 * @author r8s
 *
 */
@RestController
@RequestMapping("/authorization/users")
@Validated
public class ZanzibarUserController {

    /**
     * Setup Application config.
     */
    @Autowired
    private ApplicationConfig appConfig;

    /**
     * Create a new user.
     *
     * @param name The unique name of the user.
     */
    @RequestMapping(value = "/{name}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void create(@PathVariable("name") final String name) {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        authHandler.createUser(name);
    }

    /**
     * Delete a user.
     *
     * @param name Username to delete.
     */
    @RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void delete(@PathVariable("name") final String name) {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        authHandler.deleteUser(name);
    }

}

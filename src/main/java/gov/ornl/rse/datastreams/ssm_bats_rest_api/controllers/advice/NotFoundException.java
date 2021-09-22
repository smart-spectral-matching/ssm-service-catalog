package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
    /**
     * Required for implementing Serializable.
     */
    private static final long serialVersionUID = 1000L;
}

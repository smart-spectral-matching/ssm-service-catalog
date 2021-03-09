package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import springfox.documentation.annotations.ApiIgnore;

/**
 * Class which serves mostly to obfuscate the default error page when navigating
 * to a 404 page, and return JSON instead of HTML.
 *
 * While it can also serve as a fallback on other 4xx and 5xx errors, those
 * should be handled by the ControllerAdvice instead.
 */
@RestController
@ApiIgnore
public class ErrorControllerImpl implements ErrorController {

    /**
     * Default error mapping used by Spring Boot.
     *
     * Note: if changing the property `server.error.path`,
     * update this value to match.
     */
    private static final String ERROR_PATH = "/error";

    /**
     * @param request HTTP Request information
     * @return what will be displayed on the default error page
     */
    @RequestMapping(ERROR_PATH)
    public ResponseEntity<Object> errorMessage(final HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // dispatch code will actually be null for the string literal "/error"
        Object dispatchCode = request.getAttribute(
            RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = dispatchCode == null
            ? HttpStatus.NOT_FOUND
            : HttpStatus.resolve((int) dispatchCode);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", status.value() == HttpStatus.NOT_FOUND.value()
            ? "This URL doesn't have anything"
            : status.getReasonPhrase());
        return new ResponseEntity<>(body, headers, status);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }

}

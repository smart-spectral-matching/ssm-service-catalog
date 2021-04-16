package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers.advice;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.DefaultProblem;
import org.zalando.problem.Problem;
import org.zalando.problem.ProblemBuilder;
import org.zalando.problem.Status;
import org.zalando.problem.StatusType;
import org.zalando.problem.spring.web.advice.ProblemHandling;
//import org.zalando.problem.spring.web.advice.security.SecurityAdviceTrait;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

/**
 * Convert 4xx and 5xx exceptions to client-friendly JSON structures.
 * Also helps stop 4xx errors from actually throwing a Java exception.
 *
 * The error response follows RFC7807 -
 * Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807).
 */
@ControllerAdvice
public class ControllerExceptionHandler implements ProblemHandling
/* , SecurityAdviceTrait */ {

    /**
     * Post-process the Problem payload to modify properties as needed.
     */
    @Override
    public ResponseEntity<Problem> process(
        @Nullable final ResponseEntity<Problem> entity,
        final NativeWebRequest request
    ) {
        if (entity == null) {
            return null;
        }
        Problem problem = entity.getBody();
        if (!(problem instanceof ConstraintViolationProblem
            || problem instanceof DefaultProblem)) {
            return entity;
        }

        ProblemBuilder builder = Problem.builder()
            .withStatus(problem.getStatus())
            .withTitle(problem.getTitle());

        if (problem instanceof ConstraintViolationProblem) {
            builder.with("violations", ((ConstraintViolationProblem) problem)
                .getViolations().stream().map(v -> {
                // only post the literal field name as it appears in the API
                String[] messageParts = v.getField().split("\\.");
                Violation violation = new Violation(
                    messageParts[messageParts.length - 1],
                    v.getMessage()
                );
                return violation;
            }).collect(Collectors.toList())).withDetail("Validation Errors");
        } else {
            builder.withCause(((DefaultProblem) problem).getCause())
                .withType(problem.getType())
                .withDetail(problem.getDetail())
                .withInstance(problem.getInstance());
            problem.getParameters().forEach(builder::with);
        }
        return new ResponseEntity<>(
            builder.build(),
            entity.getHeaders(),
            entity.getStatusCode());
    }

    /**
     * ${@inheritDoc}
     *
     * Catch exceptions specifically from @Valid annotation violations.
     *
     * @param ex caught Exception
     * @param request HTTP request
     * @return RFC7807 response
     */
    @Override
    public ResponseEntity<Problem> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex,
            @Nonnull final NativeWebRequest request) {
        BindingResult result = ex.getBindingResult();
        List<FieldError> fieldErrors = result.getFieldErrors().stream()
            .map(f -> new FieldError(f.getObjectName(), f.getField(),
                StringUtils.isNotBlank(f.getDefaultMessage())
                ? f.getDefaultMessage()
                : f.getCode()))
            .collect(Collectors.toList());

        Problem problem = Problem.builder()
            .withTitle("Validation Error")
            .withStatus(defaultConstraintViolationStatus())
            .withDetail("Invalid parameter provided in query or in path")
            .with("fieldErrors", fieldErrors)
            .build();
        return create(ex, problem, request);
    }

    /**
     * ${@inheritDoc}
     *
     * Catch exceptions specifically from bad type conversions (
     * i.e. NumberFormatException).
     *
     * @param ex caught Exception
     * @param request HTTP request
     * @return RFC7807 response
     */
    @Override
    public ResponseEntity<Problem> handleTypeMismatch(
            final TypeMismatchException ex,
            @Nonnull final NativeWebRequest request) {
        String detail = "Value '" + ex.getValue() + "' is not a valid "
        + getMaskedTypeName(ex.getRequiredType().getSimpleName());
        if (StringUtils.isNotBlank(ex.getPropertyName())) {
            detail += " for property '" + ex.getPropertyName() + "'";
        }
        Problem problem = Problem.builder()
            .withTitle("Invalid Request Parameter Type")
            .withStatus(Status.BAD_REQUEST)
            .withDetail(detail)
            .build();
        return create(ex, problem, request);
    }

    /**
     * ${@inheritDoc}
     *
     * Pre-processing for generic types which are not explicitly
     * annotated with @ExceptionHandler or are not ConstraintViolations.
     */
    @Override
    public ProblemBuilder prepare(
        final Throwable throwable,
        final StatusType status,
        final URI type) {
        return Problem.builder()
            .withType(type)
            .withTitle(status.getReasonPhrase())
            .withStatus(status)
            .withDetail(throwable.getMessage())
            .withCause(Optional.ofNullable(throwable.getCause())
            .filter(cause -> isCausalChainsEnabled())
            .map(this::toProblem).orElse(null));
    }

    /**
     * Mask stringified type names to make it less obvious
     * the API is Spring-Boot.
     * Also, change Java types to match the OpenAPI spec.
     *
     * More info: https://swagger.io/docs/specification/data-models/data-types/
     *
     * @param name simple name of the Java class being referenced
     * @return masked type name
     */
    private String getMaskedTypeName(final String name) {
        switch (name.toLowerCase(Locale.getDefault())) {
            // don't directly use byte or short as request parameters
            // because these are not supported by the OpenAPI spec
            case "int":
            case "integer":
            case "byte":
            case "short":
                return "integer";
            // Lists and Sets are treated as arrays
            case "array":
            case "list":
            case "set":
                return "array";
            default:
                return name.toLowerCase(Locale.getDefault());
        }
    }

}

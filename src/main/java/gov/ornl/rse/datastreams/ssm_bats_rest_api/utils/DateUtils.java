package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class relating to dates and timestamps.
 */
public final class DateUtils {

    /**
     * Common Date-Time formatter to use for any timestamps.
     */
    public static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");

    /**
     * Static methods only.
     */
    private DateUtils() {

    }

    /**
     * @return current timestamp formatted with common formatter.
     */
    public static String now() {
        return FORMATTER.format(ZonedDateTime.now(ZoneId.from(ZoneOffset.UTC)));
    }
}

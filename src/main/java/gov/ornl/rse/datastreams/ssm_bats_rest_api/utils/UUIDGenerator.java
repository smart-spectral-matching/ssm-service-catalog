package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;


public final class UUIDGenerator {
    /**
     * Constructor set to private since this is a utility class.
    */
    private UUIDGenerator() { };

    /**
     * Hex array to use for generating UUID.
    */
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Shift value for hex characters from hex array.
    */
    private static final int SHIFT = 4;

    /**
     * Mask for 8 bit for index value.
    */
    private static final int MASK_INDEX = 0xFF;

    /**
     * Mask for 8 bit for index value.
    */
    private static final int MASK_HEX_ARRAY = 0x0F;

    /**
     * Valid regex for a generated UUID.
     */
    public static final String UUID_REGEX = "^[A-F0-9]+$";

    /**
     * Returns the hex for a given set of bytes.
     *
     * @param bytes Bytes to convert to hex
     * @return      Hex of the given bytes as a string
    */
    private static String bytesToHex(final byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & MASK_INDEX;
            hexChars[j * 2] = HEX_ARRAY[v >>> SHIFT];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & MASK_HEX_ARRAY];
        }
        return new String(hexChars);
    }

    /**
     * Returns a unique UUID as a string.
     *
     * @return Random UUID as string
    */
    public static String generateUUID()
        throws
            NoSuchAlgorithmException,
            UnsupportedEncodingException {
        MessageDigest salt = MessageDigest.getInstance("SHA-256");
        salt.update(
            UUID.randomUUID()
                .toString()
                .getBytes("UTF-8"));
        String digest = bytesToHex(salt.digest());
        return digest;
    }
}

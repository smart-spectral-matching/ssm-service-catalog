package gov.ornl.rse.datastreams.ssm_bats_rest_api;

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
     * 0xFF - Mask for 8 bit values.
    */
    private static final int MASKER = 0xFF;

    private static String bytesToHex(final byte[] bytes) {
        final int FOUR = 4;
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & MASKER;
            hexChars[j * 2] = HEX_ARRAY[v >>> FOUR];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Returns a unique UUID.
     *
     * @return Random UUID as string
    */
    public static String generateUUID() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest salt = MessageDigest.getInstance("SHA-256");
        salt.update(
            UUID.randomUUID()
                .toString()
                .getBytes("UTF-8"));
        String digest = bytesToHex(salt.digest());
        return digest;
    }
}

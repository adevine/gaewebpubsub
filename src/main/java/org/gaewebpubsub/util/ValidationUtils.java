package org.gaewebpubsub.util;

/**
 * Default algorithm for creating and validating validation tokens. See the ValidationFilter for more information on
 * how this can be used.
 *
 * @see org.gaewebpubsub.web.ValidationFilter
 */
public class ValidationUtils {
    /**
     * Creates a secure validation token. The format of the token is
     * <pre>
     * timestamp + "|" + SecureHash.hash(timestamp + privateKey)
     * </pre>
     * @param privateKey The privateKey is used to encrypt the token. Thus, this key should be sufficiently long and
     *                   random (e.g. a UUID).
     * @param timestamp  The Java timestamp (i.e. System.currentTimeMillis()) to use in creating the token.
     * @return A web-safe token string.
     */
    public static String createValidationToken(String privateKey, long timestamp) {
        return timestamp + "|" + SecureHash.hash(timestamp + privateKey);
    }

    /**
     * Creates a secure validation token using now as the timestamp.
     */
    public static String createValidationToken(String privateKey) {
        return createValidationToken(privateKey, System.currentTimeMillis());
    }

    /**
     * Determines whether a given token is valid.
     *
     * @param privateKey The private key (previously used to encode the token with createValidationToken).
     * @param token      The token previously created with createValidationToken.
     * @param minTimestamp The minimum acceptable timestamp (parsed from the token) that is considered valid
     * @param maxTimestamp The maximum acceptable timestamp (parsed from the token) that is considered valid
     * @return true if the token is valid, false otherwise
     */
    public static boolean isTokenValid(String privateKey, String token, long minTimestamp, long maxTimestamp) {
        if (token == null) {
            return false;
        }

        String[] timestampAndHash = token.split("\\|");
        if (timestampAndHash.length != 2) {
            //then token didn't contain proper parts
            return false;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampAndHash[0]);
        } catch (NumberFormatException nfe) {
            return false;
        }

        if (timestamp < minTimestamp || timestamp > maxTimestamp) {
            return false;
        }

        return token.equals(createValidationToken(privateKey, timestamp));
    }

    /**
     * Same as the other isTokenValid method but allows any timestamp value.
     */
    public static boolean isTokenValid(String privateKey, String token) {
        return isTokenValid(privateKey, token, Long.MIN_VALUE, Long.MAX_VALUE);
    }
}

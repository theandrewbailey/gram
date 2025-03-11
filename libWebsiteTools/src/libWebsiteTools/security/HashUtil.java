package libWebsiteTools.security;

import com.password4j.Argon2Function;
import com.password4j.Password;
import com.password4j.types.Argon2;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import libWebsiteTools.JVMNotSupportedError;

/**
 *
 * @author alpha
 */
public final class HashUtil {

    public static final Pattern ARGON2_ENCODING_PATTERN = Pattern.compile("^\\$(?<type>\\w*?)\\$v=(?<v>\\d*?)\\$m=(?<m>\\d*?),t=(?<t>\\d*?),p=(?<p>\\d*?)\\$(?<salt>[A-Za-z0-9\\+\\/\\=]*?)\\$(?<hash>[A-Za-z0-9\\+\\/\\=]*?)$");

    public HashUtil() {
        throw new UnsupportedOperationException("You cannot instantiate this class");
    }

    /**
     * @return SHA-256 MessageDigest
     */
    public static MessageDigest getSHA256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException x) {
            throw new JVMNotSupportedError(x);
        }
    }

    /**
     * @param toHash to hash with SHA-256
     * @return base64 SHA-256 hash
     */
    public static String getSHA256Hash(byte[] toHash) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(toHash));
        } catch (NoSuchAlgorithmException x) {
            throw new JVMNotSupportedError(x);
        }
    }

    /**
     * @param toHash to hash with SHA-256
     * @return base64 SHA-256 hash
     */
    public static String getSHA256Hash(String toHash) {
        try {
            return getSHA256Hash(toHash.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException enc) {
            throw new JVMNotSupportedError(enc);
        }
    }

    /**
     *
     * @param key key for HMAC-SHA-256
     * @param data data to HMAC-SHA-256
     * @return string of base64 HMAC-SHA-256
     */
    public static String getHmacSHA256Hash(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(hmac.doFinal(data.getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException ex) {
            throw new JVMNotSupportedError(ex);
        } catch (UnsupportedEncodingException ex) {
            throw new JVMNotSupportedError(ex);
        } catch (InvalidKeyException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Wraps getArgon2WithParameters(String, Matcher) with the correct Matcher.
     *
     * @param parameters an argon2 hash
     * @return an Argon2Function object ready to use with
     * com.​password4j.Password builders
     */
    public static Argon2Function getArgon2WithParameters(String parameters) {
        return getArgon2WithParameters(parameters, ARGON2_ENCODING_PATTERN.matcher(parameters));
    }

    /**
     * Reads an argon2 hash and returns a Argon2Function object having the same
     * parameters. Salt length must be specified separately.
     *
     * @param parameters an argon2 hash like
     * $argon2id$v=19$m=9999,t=2,p=1$...........$...........
     * @param regex ARGON2_ENCODING_PATTERN.matcher(parameters)
     * @return an Argon2Function object ready to use with
     * com.​password4j.Password builders
     */
    public static Argon2Function getArgon2WithParameters(String parameters, Matcher regex) {
        try {
            if (regex.find()) {
                int memory = Integer.parseInt(regex.group("m"));
                int version = Integer.parseInt(regex.group("v"));
                int iterations = Integer.parseInt(regex.group("t"));
                int parallelism = Integer.parseInt(regex.group("p"));
                int length = Base64.getDecoder().decode(regex.group("hash")).length;
                Argon2 type;
                switch (regex.group("type")) {
                    case "argon2i":
                        type = Argon2.I;
                        break;
                    case "argon2d":
                        type = Argon2.D;
                        break;
                    case "argon2id":
                    default:
                        type = Argon2.ID;
                }
                return Argon2Function.getInstance(memory, iterations, parallelism, length, type, version);
            } else {
                throw new IllegalArgumentException("Parameter string doesn't match argon2 encoding pattern: " + parameters);
            }
        } catch (NumberFormatException n) {
            throw new IllegalArgumentException("Can't get argon2 parameters from parameter string: " + parameters);
        }
    }

    /**
     * Returns an encoded argon2 hash.
     *
     * @param parameters a string that matches ARGON2_ENCODING_PATTERN to read
     * parameters from, or null for defaults
     * @param password password (presumably) to be hashed
     * @return argon2 hash
     */
    public static String getArgon2Hash(String parameters, String password) {
        try {
            Matcher regex = ARGON2_ENCODING_PATTERN.matcher(parameters);
            Argon2Function argon2 = getArgon2WithParameters(parameters, regex);
            int saltLen = Base64.getDecoder().decode(regex.group("salt")).length;
            return Password.hash(password.getBytes("UTF-8")).addRandomSalt(saltLen).with(argon2).getResult();
        } catch (UnsupportedEncodingException ex) {
            throw new JVMNotSupportedError(ex);
        } catch (NumberFormatException n) {
            throw new IllegalArgumentException("Can't get argon2 parameters from parameter string: " + parameters);
        }
    }

    /**
     * Verifies that the provided password created the provided encoded hash.
     *
     * @param encoded an argon2 encoded hash
     * @param password password to test
     * @return did it blend?
     * @see HashUtil.getArgon2Hash
     */
    public static boolean verifyArgon2Hash(String encoded, String password) {
        return Password.check(password, encoded).with(getArgon2WithParameters(encoded));
    }
}

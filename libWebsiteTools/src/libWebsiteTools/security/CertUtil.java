package libWebsiteTools.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.io.File;
import java.util.HexFormat;
import libWebsiteTools.JVMNotSupportedError;

/**
 *
 * @author alpha
 */
public class CertUtil {

    /**
     * contains root CAs and intermediates, but not the server's (leaf)
     * certificate
     */
    private final MultivaluedHashMap<X500Principal, X509Certificate> CERTIFICATE_STORE = new MultivaluedHashMap<>();
    private static final Logger LOG = Logger.getLogger(CertUtil.class.getName());
    private static final String SCT_EXTENSION = "1.3.6.1.4.1.11129.2.4.2";
    private X509Certificate subject;
    private Date certExpDate;
    private KeyStore customCerts;

    public CertUtil(Certificate[] certs) {
        try {
            customCerts = KeyStore.getInstance(KeyStore.getDefaultType());
            customCerts.load(null, null);
            for (Certificate c : certs) {
                if (c instanceof X509Certificate) {
                    X509Certificate x509 = (X509Certificate) c;
                    CERTIFICATE_STORE.add(x509.getSubjectX500Principal(), x509);
                    customCerts.setEntry(x509.getSubjectX500Principal().getName(), new KeyStore.TrustedCertificateEntry(c), null);
                }
            }
            addStore(getTrustStore(), CERTIFICATE_STORE);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * open keystores on instantiation
     */
    public CertUtil() {
        try {
            Enumeration<String> serverAliases = getServerKeystore().aliases();
            for (String aliasName = serverAliases.nextElement(); serverAliases.hasMoreElements(); aliasName = serverAliases.nextElement()) {
                try {
                    String prop = System.getProperty("javax.net.ssl.keyStorePassword");
                    if (null == prop) {
                        return;
                    }
                    KeyStore.PrivateKeyEntry ksent = (KeyStore.PrivateKeyEntry) getServerKeystore().getEntry(aliasName, new KeyStore.PasswordProtection(prop.toCharArray()));
                    X509Certificate subCert = (X509Certificate) ksent.getCertificate();
                    CERTIFICATE_STORE.add(subCert.getSubjectX500Principal(), subCert);
                    for (Certificate genericCert : ksent.getCertificateChain()) {
                        try {
                            if (subCert.equals(genericCert)) {
                                continue;
                            }
                            X509Certificate cert = (X509Certificate) genericCert;
                            CERTIFICATE_STORE.add(cert.getSubjectX500Principal(), cert);
                        } catch (Exception ex) {
                        }
                    }
                } catch (RuntimeException ex) {
                    LOG.log(Level.SEVERE, "Can't add certificate to keystore: " + aliasName, ex);
                }
            }
            addStore(getTrustStore(), CERTIFICATE_STORE);
        } catch (NoSuchAlgorithmException ex) {
            throw new JVMNotSupportedError(ex);
        } catch (UnrecoverableEntryException | KeyStoreException ex) {
            throw new RuntimeException("Something went wrong with your keystore.", ex);
        }
    }

    public static void addStore(KeyStore keys, MultivaluedHashMap<X500Principal, X509Certificate> store) throws KeyStoreException {
        Enumeration<String> aliases = keys.aliases();
        while (aliases.hasMoreElements()) {
            try {
                KeyStore.Entry entry = keys.getEntry(aliases.nextElement(), null);
                X509Certificate cert = (X509Certificate) ((KeyStore.TrustedCertificateEntry) entry).getTrustedCertificate();
                store.add(cert.getSubjectX500Principal(), cert);
            } catch (Exception ex) {
            }
        }
    }

    public void verifyCertificate(String certName) {
        if (null != certName) {
            List<CertPath<X509Certificate>> serverCertificateChain = getServerCertificateChain(certName);
            subject = serverCertificateChain.get(0).getCertificates().get(0);
            if (!CertUtil.isValid(subject)) {
                throw new RuntimeException(String.format("Your server's certificate has expired.\n%s", new Object[]{getSubject().getSubjectX500Principal().toString()}));
            }
            certExpDate = getEarliestExperation(serverCertificateChain);
        } else {
            throw new RuntimeException("No certificate name set.");
        }
    }

    public X509Certificate getSubject() {
        return subject;
    }

    public Date getCertExpDate() {
        return certExpDate;
    }

    public static Date getEarliestExperation(List<CertPath<X509Certificate>> chains) {
        Date certDate = null;
        for (CertPath<X509Certificate> certificateChain : chains) {
            for (X509Certificate cert : certificateChain.getCertificates()) {
                if (CertUtil.isValid(cert)) {
                    if (null == certDate || cert.getNotAfter().before(certDate)) {
                        certDate = cert.getNotAfter();
                    }
                } else {
                    throw new RuntimeException(String.format("A certificate in your trust chain is not valid!\n%s", new Object[]{cert.getSubjectX500Principal().toString()}));
                }
            }
        }
        return certDate;
    }

    /**
     * load a keystore with the given filename and use the password to decrypt
     * it.
     *
     * @param filename
     * @param pass
     * @return KeyStore object of the given filename, or null if something went
     * wrong
     */
    public static KeyStore getKeystore(String filename, char[] pass) {
        try {
            return KeyStore.getInstance(new File(filename), pass);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException ex) {
            return null;
        }
    }

    /**
     *
     * @return keystore this server uses. WARNING: THIS CONTAINS ANY PRIVATE KEY
     * BEING USED FOR THIS SERVER!
     */
    private static KeyStore getServerKeystore() {
        if (null != System.getProperty("javax.net.ssl.keyStorePassword", null)) {
            return getKeystore(System.getProperty("javax.net.ssl.keyStore"), System.getProperty("javax.net.ssl.keyStorePassword").toCharArray());
        }
        return getKeystore(System.getProperty("javax.net.ssl.keyStore"), null);
    }

    /**
     *
     * @return keystore that contains trusted root CAs
     */
    private static KeyStore getTrustStore() {
        if (null != System.getProperty("javax.net.ssl.trustStorePassword", null)) {
            return getKeystore(System.getProperty("javax.net.ssl.trustStore"), System.getProperty("javax.net.ssl.trustStorePassword").toCharArray());
        }
        return getKeystore(System.getProperty("javax.net.ssl.trustStore"), null);
    }

    /**
     * look up certificate in keystore
     *
     * @param name
     * @return requested certificate, or null
     * @see GuardFilter.CERTIFICATE_NAME
     */
    public static X509Certificate getServerCertificate(String name) {
        try {
            return (X509Certificate) getServerKeystore().getCertificate(name);
        } catch (KeyStoreException | NullPointerException ex) {
            return null;
        }
    }

    /**
     *
     * @param certificateName name (alias) of the desired certificate
     * @return list (LinkedHashSet) containing certificates of the trust chain
     */
    public List<CertPath<X509Certificate>> getServerCertificateChain(String certificateName) {
        try {
            X509Certificate subCert;
            if (null != customCerts) {
                KeyStore.TrustedCertificateEntry ent = (KeyStore.TrustedCertificateEntry) customCerts.getEntry(certificateName, null);
                subCert = (X509Certificate) ent.getTrustedCertificate();
            } else {
                KeyStore store = getServerKeystore();
                KeyStore.PrivateKeyEntry ksent = (KeyStore.PrivateKeyEntry) store.getEntry(certificateName, new KeyStore.PasswordProtection(System.getProperty("javax.net.ssl.keyStorePassword").toCharArray()));
                subCert = (X509Certificate) ksent.getCertificate();
            }
            return new ArrayList<>(getChain(subCert, null));
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     *
     * @param subject certificate in question
     * @param chain existing chain to append to, null creates new chain
     * @return certificates of keys that signed the subject (and those
     * certificates that signed those...)
     */
    public Set<CertPath<X509Certificate>> getChain(X509Certificate subject, LinkedHashSet<X509Certificate> chain) {
        if (null == chain) {
            chain = new LinkedHashSet<>();
        } else if (Math.max(10, CERTIFICATE_STORE.size()) < chain.size()) {
            throw new RuntimeException(String.format("Certificate chain for %s doesn't terminate", new Object[]{subject.getSubjectX500Principal()}));
        }
        Set<CertPath<X509Certificate>> paths = new HashSet<>();
        chain.add(subject);
        try {
            for (X509Certificate authority : CERTIFICATE_STORE.get(subject.getIssuerX500Principal())) {
                if (verify(subject, authority.getPublicKey())) {
                    if (isSelfSigned(authority)) {
                        // root reached because root certificates are self signed
                        chain.add(authority);
                        paths.add(new CertPath<>(new ArrayList<>(chain)));
                        chain.remove(authority);
                        continue;
                    }
                    Set<CertPath<X509Certificate>> parents = getChain(authority, chain);
                    if (null != parents) {
                        paths.addAll(parents);
                    }
                }
            }
        } catch (NullPointerException n) {
            return null;
        }
        chain.remove(subject);
        return paths;
    }

    /**
     *
     * @param subject
     * @param publicKey
     * @return did the private key of publicKey sign subject?
     */
    public static boolean verify(X509Certificate subject, PublicKey publicKey) {
        for (Provider p : Security.getProviders()) {
            try {
                subject.verify(publicKey, p);
                return true;
            } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException | CertificateException ex) {
                // if a thing didn't work, try the next thing.
                // if all the things don't work, just say no.
                // it's just not worth getting all excited about it.
            }
        }
        return false;
    }

    /**
     *
     * @param certificate
     * @return is certificate valid now?
     */
    public static boolean isValid(X509Certificate certificate) {
        try {
            certificate.checkValidity();
            return true;
        } catch (CertificateExpiredException | CertificateNotYetValidException ex) {
            return false;
        }
    }

    public static boolean isSelfSigned(X509Certificate certificate) {
        return certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal()) && verify(certificate, certificate.getPublicKey());
    }

    public static boolean hasSCT(X509Certificate certificate) {
        return null != certificate.getExtensionValue(SCT_EXTENSION);
    }

    /**
     *
     * @param certificate
     * @return pretty strings of select fields of certificate
     */
    public static LinkedHashMap<String, String> formatCert(X509Certificate certificate) {
        try {
            LdapName subject = new LdapName(certificate.getSubjectX500Principal().getName("RFC2253"));
            LinkedHashMap<String, String> fields = new LinkedHashMap<>();
            fields.put("Subject", getRdnValue(subject, "CN"));
            fields.put("Principal", subject.toString());
            if (null != certificate.getSubjectAlternativeNames()) {
                fields.put("Alternates", Arrays.toString(certificate.getSubjectAlternativeNames().toArray()));
            }
            fields.put("Expires", certificate.getNotAfter().toString());
            LdapName issuer = new LdapName(certificate.getIssuerX500Principal().getName("RFC2253"));
            //fields.put("Key (" + certificate.getPublicKey().getAlgorithm() + ")", insertChars(HashUtil.getHex(certificate.getPublicKey().getEncoded()), ' '));
            //fields.put("Fingerprint", HashUtil.getHex(HashUtil.getSHA256().digest(certificate.getEncoded())).toLowerCase());
            fields.put("Fingerprint", HexFormat.of().withLowerCase().formatHex(HashUtil.getSHA256().digest(certificate.getEncoded())));
            fields.put("Pin", getCertificatePinSHA256(certificate));
            if (hasSCT(certificate)) {
                fields.put("SCT", "present");
            }
            fields.put("Issuer", isSelfSigned(certificate) ? getRdnValue(issuer, "CN") + " (self-signed)" : getRdnValue(issuer, "CN"));
            fields.put("Crypto", certificate.getPublicKey().getAlgorithm() + " + " + certificate.getSigAlgName());
            //fields.put("Signature (" + certificate.getSigAlgName() + ")", insertChars(HashUtil.getHex(certificate.getSignature()), ' '));
            return fields;
        } catch (CertificateEncodingException | CertificateParsingException | InvalidNameException ex) {
        }
        return null;
    }

    /**
     *
     * @param principal
     * @param field
     * @return field from principal
     */
    public static String getRdnValue(LdapName principal, String field) {
        for (Rdn subname : principal.getRdns()) {
            if (subname.getType().equals(field)) {
                return Rdn.unescapeValue(subname.getValue().toString()).toString();
            }
        }
        return null;
    }

    /**
     *
     * @param hex
     * @param insertion
     * @return pairs of hex separated by insertion, newlines every 16 pairs
     */
    public static String insertChars(String hex, char insertion) {
        if (2 > hex.length()) {
            return hex;
        }
        StringBuilder output = new StringBuilder(hex.length() * 2);
        char[] chars = hex.toCharArray();
        output.append(chars[0]).append(chars[1]);
        for (int i = 2; i < chars.length; i++) {
            if (0 == i % 32) {
                output.append('\n');
            }
            output.append(insertion).append(chars[i++]);
            if (i < chars.length) {
                output.append(chars[i]);
            }
        }
        return output.toString();
    }

    /**
     *
     * @param certificate
     * @return base64 of sha256 hash of certificate key
     */
    public static String getCertificatePinSHA256(Certificate certificate) {
        return HashUtil.getSHA256Hash(certificate.getPublicKey().getEncoded());
    }
}

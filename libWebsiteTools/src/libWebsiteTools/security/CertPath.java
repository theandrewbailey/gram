package libWebsiteTools.security;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.x500.X500Principal;

/**
 *
 * @author alpha
 * @param <k>
 */
public class CertPath<k extends X509Certificate> extends java.security.cert.CertPath {

    private final List<k> certificates;

    public CertPath(List<k> certificates) {
        super(certificates.get(0).getType());
        this.certificates = certificates;
    }

    /**
     *
     * @return earliest expiration date of all certificates in chain. Only works
     * for X509 Certificates.
     */
    public Date getExpiration() {
        Date earliest = null;
        for (Certificate c : certificates) {
            if (c instanceof X509Certificate && (null == earliest || ((X509Certificate) c).getNotAfter().before(earliest))) {
                earliest = ((X509Certificate) c).getNotAfter();
            }
        }
        return earliest;
    }

    public String getCertExpirationDays(X509Certificate x509) {
        Long days = (x509.getNotAfter().getTime() - new Date().getTime()) / 86400000L;
        return days.toString();
    }

    public String getChainExpirationDays() {
        Long days = (getExpiration().getTime() - new Date().getTime()) / 86400000L;
        return days.toString();
    }

    public String getRootSubject() {
        try {
            X500Principal certificate = getCertificates().get(getCertificates().size() - 1).getSubjectX500Principal();
            LdapName subject = new LdapName(certificate.getName("RFC2253"));
            return CertUtil.getRdnValue(subject, "CN");
        } catch (InvalidNameException vm) {
            throw new RuntimeException(vm);
        }
    }

    @Override
    public Iterator<String> getEncodings() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getEncoded() throws CertificateEncodingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getEncoded(String string) throws CertificateEncodingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<k> getCertificates() {
        return certificates;
    }

}

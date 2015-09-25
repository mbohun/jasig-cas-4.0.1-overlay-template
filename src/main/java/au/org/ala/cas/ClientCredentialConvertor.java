package au.org.ala.cas;

import org.jasig.cas.authentication.Credential;
import org.jasig.cas.support.pac4j.authentication.principal.ClientCredential;

public interface ClientCredentialConvertor {
    public Credential convert(final ClientCredential clientCredential);
    public boolean supports(final ClientCredential clientCredential);
}

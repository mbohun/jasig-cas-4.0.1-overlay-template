package au.org.ala.cas;

import org.jasig.cas.authentication.Credential;
import org.jasig.cas.support.pac4j.authentication.principal.ClientCredential;
import org.pac4j.core.profile.UserProfile;
import au.org.ala.cas.AttributeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientCredentialConvertorALA implements ClientCredentialConvertor {

    /** Log instance. */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Credential convert(final ClientCredential clientCredential) {
	final UserProfile userProfile = clientCredential.getUserProfile();
	logger.debug("userProfile : {}", userProfile);

	final String email = AttributeParser.lookup("email", userProfile);
	logger.debug("email : {}", email);
	
	// ALA is using email to query the existing SQL DB for attributes
	return new Credential() {
	    public String getId() {
		return email;
	    }
	};
    }

    // TODO: although it is possible to just return true here, we are going to verify:
    // - we got a credential with valid email (for example Twitter OAuth credential does NOT contain an email,
    //   - unless you apply for it in writing/email, AND
    //   - your application is approved)
    // - OR we can do here a more generic check, that this credential came from pac4j:
    //   - TwitterClient
    //   - FacebookClient
    //   - Google2Client
    //   - etc.
    //   and do the check for valid email later?
    //
    public boolean supports(final ClientCredential clientCredential) {
	return true;
    }
}

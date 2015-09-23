package au.org.ala.cas.authentication.principal;

import org.jasig.cas.authentication.principal.PrincipalResolver;
import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.authentication.Credential;
import org.jasig.cas.support.pac4j.authentication.principal.ClientCredential;
import org.pac4j.core.profile.UserProfile;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import au.org.ala.cas.UserCreator;
import au.org.ala.cas.AttributeParser;

/**
 * NOTE: This is only a concept/experiment for now; it's goals are as follows:
 *
 * 1. Get rid of org.jasig.cas.support.pac4j.authentication.handler.support.ALAClientAuthenticationHandler
 * 2. "Revert" to the default pac4j AuthenticationHandler so the pac4j Authentication Handler will do
 *    authentication ONLY.
 *    NOTE: This can be problematic, resp. we will have to implement it ourselves, because the "default"
 *          pac4j AuthenticationHandler actually introduced this problem of grouping together authentication
 *          with ALL the other post authentication steps, not offering a simple place for customization).
 *          The problem is mainly in the assumption that the default pac4j Authentication Handler should
 *          release (forward in fact) whatever attributes it received from Facebook, Twitter, Google OAuth,
 *          OpenID, SAML to CAS clients.
 *          Thas is *NOT* the case for most of the existing CAS users, that have been using CAS for years,
 *          prior to Facebook, Twitter, Google, etc. became popular for "SignUp/SignIn".
 *          Many, if not most, existing CAS users have been using CAS with LDAP, or SQL DB and their CAS
 *          client applications do expect to receive set of custom CAS attributes (different from
 *          the attributes send by Facebook, Twitter, etc.).
 *          There seem to be a requirement for a pac4j-attributes-to-custom-attributes-translation.
 *          I did initially try to use (or abuse?) for this step of translation the CAS attribute filter.
 *          Hower CAS attribute filter did not work for that purpose, because CAS does NOT allow to
 *          create new attributes in the filter.
 * 3. DelegateAuthenticationPrincipalResolver (alone, or with helper classes) will provide ALL
 *    the post-authentication steps, trying to separate the common/generic functionality from the project
 *    (in this case Atlas of Living Australia) specific details. The project specific details should be
 *    nicely moved into "some custom bean-s", CAS users can easily implement themselves.
 *
 * @author Martin Bohun
 * @since 4.0
 *
 */
public class DelegateAuthenticationPrincipalResolver implements PrincipalResolver {

    /** Log instance. */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    static final Pattern EMAIL_PATTERN  = Pattern.compile("^.+@.+\\..+$");

    @NotNull
    private final PrincipalResolver principalResolver;

    @NotNull
    private final UserCreator userCreator;

    /** Optional prinicpal attribute name. */
    private String principalAttributeName;

    public DelegateAuthenticationPrincipalResolver(final PrincipalResolver principalResolver,
						   final UserCreator userCreator) {
	this.principalResolver = principalResolver;
	this.userCreator = userCreator;
    }

    @Override
    public boolean supports(final Credential credential) {
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
	return true;
    }

    @Override
    public final Principal resolve(final Credential credential) {
        logger.debug("credential : {}", credential);

	final ClientCredential clientCredentials = (ClientCredential) credential;
        logger.debug("clientCredentials : {}", clientCredentials);

	final UserProfile userProfile = clientCredentials.getUserProfile();
	logger.debug("userProfile : {}", userProfile);

	// NOTE: up to this point we are generic/common, from here on it is project (ALA) specific
	final String email = AttributeParser.lookup("email", userProfile);
	logger.debug("email : {}", email);

	if (email==null || !EMAIL_PATTERN.matcher(email).matches()) {
	    logger.debug("Invalid email : {}, authentication aborted!", email);
	    return null;
	}

	// ALA is using email to query the existing SQL DB for attributes
	final Credential alaCredential = new Credential() {
		public String getId() {
		    return email;
		}
	    };

	// NOTE: this is the point where we joined/hooked into the existing principal resolver
	// get the ALA user attributes from the userdetails DB ("userid", "firstname", "lastname", "authority")
	Principal principal = this.principalResolver.resolve(alaCredential);

	// does the ALA user exist?
	if (!principal.getAttributes().containsKey("userid")) { //TODO: make this nice and configurable
	    // create a new ALA user in the userdetails DB
	    logger.debug("user {} not found in ALA userdetails DB, creating new ALA user for: {}.", email, email);
	    this.userCreator.createUser(userProfile); //TODO: we can check this for failed user creation, to be accurate

	    // re-try (we have to retry, because that is how we get the required "userid")
	    principal = this.principalResolver.resolve(alaCredential);
	    if (!principal.getAttributes().containsKey("userid")) {
		// we failed to lookup ALA user (most likely because the creation above failed), complain, throw exception, etc.
		return null;
	    }
	}

	return principal;
    }
}

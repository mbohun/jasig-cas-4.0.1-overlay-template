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
import au.org.ala.cas.PrincipalValidator;
import au.org.ala.cas.PrincipalValidatorALA; //TODO: remove this, once we turn validator into a bean

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

    @NotNull
    private final PrincipalValidator principalValidator;

    // TODO: check what and how was this used:
    /** Optional prinicpal attribute name. */
    private String principalAttributeName;

    public DelegateAuthenticationPrincipalResolver(final PrincipalResolver principalResolver,
						   final UserCreator userCreator) {
	this.principalResolver = principalResolver;
	this.userCreator = userCreator;
	this.principalValidator = new PrincipalValidatorALA(); //TODO: make this into a bean? we could have a map/list of diff validators?
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
	    logger.error("Invalid email : {}, authentication aborted!", email);
	    return null;
	}

	// ALA is using email to query the existing SQL DB for attributes
	final Credential alaCredential = new Credential() {
		public String getId() {
		    return email;
		}
	    };

	// NOTE: this is the point where we joined/hooked into the existing principal resolver

	// get the user attributes
	Principal principal = this.principalResolver.resolve(alaCredential); // credentialConvertor.convert(credential)
	logger.debug("{} resolved principal: {}, with attributes: {}",
		     this.principalResolver,
		     principal,
		     principal == null ? "N/A" : principal.getAttributes());

	// does the user alrady exist OR is this a new user?
	if (this.principalValidator.validate(principal)) {
	    // NOTE: unfortunatelly ALA's mysql stored procedure sp_get_user_attributes returns for a non-existent user
	    //       one attribute, that is then returned in Principal attributes (authority=null); therefore it seems
	    //       to be safer to provide a dedicated validator that can:
	    //       - examine the returned principal in details
	    //       - be easily customized for other projects
	    //
	    return principal;
	}

	logger.debug("user {} not found, creating new user for: {}.", email, email); // TODO: translatedCredential.getId() just in case it is not an email

	// create new user
	this.userCreator.createUser(userProfile); //TODO: we can check this for failed user creation, to be accurate

	// re-try (we have to retry, because that is how we get the required "userid")
	principal = this.principalResolver.resolve(alaCredential);
	logger.debug("{} resolved principal: {}, with attributes: {}",
		     this.principalResolver,
		     principal,
		     principal == null ? "N/A" : principal.getAttributes());

	// does the user exist now / did we succee to create the user ?
	if (this.principalValidator.validate(principal)) {
	    return principal;
	}

	// we failed to lookup the user, after we tried to create the user, so we have a problem
	logger.error("failed to create user for {}!", email);
	return null;
    }
}

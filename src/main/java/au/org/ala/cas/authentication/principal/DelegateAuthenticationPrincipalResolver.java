package au.org.ala.cas.authentication.principal;

import org.jasig.cas.authentication.principal.PrincipalResolver;
import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.authentication.Credential;
import org.jasig.cas.support.pac4j.authentication.principal.ClientCredential;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import au.org.ala.cas.UserCreator;
import au.org.ala.cas.PrincipalValidator;
import au.org.ala.cas.PrincipalValidatorALA; //TODO: remove this, once we turn validator into a bean
import au.org.ala.cas.ClientCredentialConvertor;
import au.org.ala.cas.ClientCredentialConvertorALA;

/**
 * @author Martin Bohun
 * @since 4.0
 *
 */
public class DelegateAuthenticationPrincipalResolver implements PrincipalResolver {

    /** Log instance. */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @NotNull
    private final PrincipalResolver principalResolver;

    @NotNull
    private final UserCreator userCreator;

    @NotNull
    private final PrincipalValidator principalValidator;

    @NotNull
    private final ClientCredentialConvertor credentialConvertor;

    public DelegateAuthenticationPrincipalResolver(final PrincipalResolver principalResolver,
						   final UserCreator userCreator) {
	this.principalResolver = principalResolver;
	this.userCreator = userCreator;
	this.principalValidator = new PrincipalValidatorALA(); //TODO: make this into a bean? we could have a map/list of diff validators?
	this.credentialConvertor = new ClientCredentialConvertorALA(); //TODO: make this into a bean
    }

    @Override
    public boolean supports(final Credential credential) {
	final ClientCredential clientCredential = (ClientCredential)credential;
        logger.debug("clientCredential : {}", clientCredential);

	return this.credentialConvertor.supports(clientCredential);
    }

    @Override
    public final Principal resolve(final Credential credential) {
	final ClientCredential clientCredential = (ClientCredential)credential;
        logger.debug("clientCredential : {}", clientCredential);

	// convert from pac4j Credential into a project/application specific Credential
	final Credential convertedCredential = this.credentialConvertor.convert(clientCredential);

	// try to lookup the user
	Principal principal = this.principalResolver.resolve(convertedCredential);
	logger.debug("{} resolved principal: {}, with attributes: {}",
		     this.principalResolver,
		     principal,
		     principal == null ? "N/A" : principal.getAttributes());

	// did we find the user?
	if (this.principalValidator.validate(principal)) {
	    return principal;
	}

	logger.debug("user {} not found, creating new user for: {}.",
		     convertedCredential.getId(),
		     convertedCredential.getId());

	// try to create the user
	this.userCreator.createUser(clientCredential);

	// try to lookup the user again
	principal = this.principalResolver.resolve(convertedCredential);
	logger.debug("{} resolved principal: {}, with attributes: {}",
		     this.principalResolver,
		     principal,
		     principal == null ? "N/A" : principal.getAttributes());

	// did we find the user this time?
	if (this.principalValidator.validate(principal)) {
	    return principal;
	}

	// we failed to lookup the user, after we tried to create the user, so we have a problem
	logger.error("failed to create user for {}!", convertedCredential);
	return null;
    }
}

package au.org.ala.cas;

import org.jasig.cas.authentication.principal.Principal;

public class PrincipalValidatorALA implements PrincipalValidator {
    public boolean validate(final Principal principal) {
	return (principal != null)
	    && (principal.getAttributes() != null)
	    && principal.getAttributes().containsKey("userid");
    }
}

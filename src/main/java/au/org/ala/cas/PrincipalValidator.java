package au.org.ala.cas;

import org.jasig.cas.authentication.principal.Principal;

public interface PrincipalValidator {
    public boolean validate(final Principal principal);
}

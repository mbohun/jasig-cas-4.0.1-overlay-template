package org.jasig.cas.services.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.RegisteredServiceAttributeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is to test if a custom attribute filter can be used to implement:
 * - ALA profile creation/setup (from the received Facebook/Google/etc. attributes)
 * - return custom ALA attributes back to CAS client-s
 *
 * @author Martin Bohun
 * @since 4.0.0
 * @see ALARegisteredServiceAttributeFilter
 */
public final class ALARegisteredServiceAttributeFilter implements RegisteredServiceAttributeFilter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Map<String, Object> filter(final String principalId,
				      final Map<String, Object> givenAttributes,
				      final RegisteredService registeredService) {

        final Map<String, Object> attributes = new HashMap<String, Object>();

        if (registeredService.isIgnoreAttributes()) {
            logger.debug("Service [{}] is set to ignore attribute release policy. Releasing all attributes.",
			 registeredService.getName());

            attributes.putAll(givenAttributes);

        } else {
	    logger.debug("Service [{}] is setting ALA user profile attributes.",
			 registeredService.getName());

	    final StringBuffer buffy = new StringBuffer("\n");
	    final Iterator keys = givenAttributes.keySet().iterator();
	    while (keys.hasNext()) {
		final String k = (String)keys.next();
		buffy.append("givenAttributes[" + k + "]:" + givenAttributes.get(k) + "\n");
	    }

	    logger.debug(buffy.toString());

	    /* UPDATE: this does NOT work, regardless on what you put into the result/return map, it won't allow you to:
	       - add a new attribute
	       - change a value of an existing attribute
	       we could still use this spot to check (and create if not present) ALA user profile in the userdetails sql DB, but most likely that too
	       has to be done, or better done somewhere else.

	      TODO: here we:
	       1. take the givenAttributes Map (received from facebook/google/twitter/github/etc.)
	       2. take the email address and check if the user exist in the userdetails DB ()
	          2.1 if the user does NOT exist we use the givenAttributes Map "automatically" register/create
		      an ALA user in the userdetails DB and activate her/him (like if the registered manually and confirmed the registration email)
	       3. from here on we continue using the existing emmet.sp_get_user_attributes(@p_username); where @p_username is the user email
	          we received from facebook/google/twitter/github/etc.

	    attributes.put("email",     "martin.bohun@gmail.com");
	    attributes.put("firstname", "Martin");
	    attributes.put("lastname",  "Bohun");
	    attributes.put("userid",    "2");
	    attributes.put("authority", "ROLE_USER");

	    */
        }
	
        return Collections.unmodifiableMap(attributes);
    }

}

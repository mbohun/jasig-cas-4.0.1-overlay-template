package au.org.ala.cas;

import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeParser {

    private AttributeParser() {} //this is just a stateless helper for now

    static final Map ATTRIBUTE_NAMES_LOOKUP = new HashMap() {
	    {   //  ALA                       facebook,     google,        linkedin
		put("email",     new String[]{"email",      "email",       "email-address"});
		put("firstname", new String[]{"first_name", "given_name",  "first-name"});
		put("lastname",  new String[]{"last_name",  "family_name", "last-name"});
	    }
	};

    static final public String lookup(final String alaName, final Map userAttributes) {
	final String[] alias = (String[])ATTRIBUTE_NAMES_LOOKUP.get(alaName);
	for (int i = 0; i < alias.length; i++) {
	    final Object match = userAttributes.get(alias[i]);
	    if (match != null) {
		return (String)match;
	    }
	}
	return null; //bad news - no match
    }

}

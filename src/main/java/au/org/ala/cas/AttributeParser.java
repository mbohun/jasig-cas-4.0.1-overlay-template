package au.org.ala.cas;

import org.pac4j.core.profile.UserProfile;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unchecked")
public class AttributeParser {

    /** Log instance. */
    static private final Logger logger = LoggerFactory.getLogger(AttributeParser.class);

    private AttributeParser() {} //this is just a stateless helper for now

    static final Map ATTRIBUTE_NAMES_LOOKUP = new HashMap() {
	    {   //  ALA                       facebook,     google,        linkedin
		put("email",     new String[]{"email",      "email",       "email-address"});
		put("firstname", new String[]{"first_name", "given_name",  "first-name"});
		put("lastname",  new String[]{"last_name",  "family_name", "last-name"});
	    }
	};

    static final public String lookup(final String alaName, final UserProfile userProfile) {
	final String profileTypeId = userProfile.getTypedId(); // GitHubProfile#12345

	final String profileType =
	    profileTypeId.substring(0, profileTypeId.indexOf(UserProfile.SEPARATOR)); // FacebookProfile,GitHubProfile,Google2Profile,etc.
	logger.debug("processing/parsing profile: {}", profileType);

	final Map userAttributes = userProfile.getAttributes();
	logger.debug("parsing: {}, attributes: {}", profileType, userAttributes);

	if (profileType.equals("GitHubProfile")) {
	    if (alaName.equals("email")) {
		//       WARNING: as of today (2015-06-10) GitHub is allowing to set/configure an UNVERIFIED email address in
		//       the Public profile -> Public email; this seems to allow for (at least 2 problematic scenarios):
		//       1. abuse ALA with unverified GitHub emails, BUT the next one is much scarier:
		//       2. a GitHub user (attacker), say myself (mbohun/martin.bohun@gmail.com) can add to his GitHub profile Emails
		//          victim's email address, then set the victim's email address in Public profile -> Public email. After this
		//          the attacker can login into ALA via "Login with GitHub" and the attacker is logged into ALA as the victim.
		//          The only limitation/restriction is if the (ALA) victim has an existing GitHub profile registered/using
		//          the victim's email address (the same email address the victim uses for ALA); in such scenario GitHub won't
		//          allow the attacker to use/add an email address that is alrady used by another GitHub user (the victim).
		//          This leads to the conclusion that the safest solution is to ALWAYS ignore the GitHub profile email address,
		//          use the access_token to REST HTTP GET https://api.github.com/user/emails?access_token=${access_token} the
		//          array/set of GitHub user's emails, and use the email address that is: primary AND verified.
		//

		final String githubAccessToken = (String)userAttributes.get("access_token");
		if (githubAccessToken == null) {
		    logger.debug("can't get a valid GitHub access_token!");
		    return null;
		}

		final String githubEmailREST =
		    "https://api.github.com/user/emails?access_token=" + githubAccessToken;

		final String result = AttributeParser.HTTP_GET(githubEmailREST);
		logger.debug("HTTP_GET {}; result: {}", githubEmailREST, result);

		try {

		    final JSONArray emails = new JSONArray(result);
		    logger.debug("GitHub emails: {}", emails);

		    for (int i = 0; i < emails.length(); i++) {
			final JSONObject emailRecord = emails.getJSONObject(i);
			if (emailRecord.getBoolean("primary")) { //TODO: enforce verified email: && emailRecord.getBoolean("verified")
			    final String email = emailRecord.getString("email");
			    logger.debug("using GitHub email: {}", email);
			    return email;
			}
		    }

		 } catch (Throwable e) {
		    logger.debug("error parsing github JSON response: {}", e.getMessage());
		    return null;
		 }

		// we did NOT find an email? not sure how likely is that, maybe later: we did NOT find any VERIFIED email

	    } else if (alaName.equals("firstname")) {
		final String name = (String)userAttributes.get("name");
		logger.debug("getting firstname from name: {}", name);

		if ((name == null) || (name.length() == 0)) {
		    return (String)userAttributes.get("login");
		}

		final String[] nameStrings = name.split("\\s");
		return nameStrings[0];

	    } else if (alaName.equals("lastname")) {
		final String name = (String)userAttributes.get("name");
		logger.debug("getting lastname from name: {}", name);

		if ((name == null) || (name.length() == 0)) {
		    return (String)userAttributes.get("login");
		}

		final String[] nameStrings = name.split("\\s");
		logger.debug("nameStrings: {}, nameStrings.length: {}", nameStrings, nameStrings.length);

		if (nameStrings.length == 1) {
		    return nameStrings[0];

		} else {
		    final StringBuffer sb = new StringBuffer();
		    for (int i = 1; i < nameStrings.length; i++) {
			sb.append(nameStrings[i]);
			sb.append(" ");
		    }
		    return sb.toString().trim(); //trim the trailing white space at the end

		}

	    } else {
		logger.debug("error, unknown attribute: {} requested!", alaName);
		return null;
	    }

	} else if (profileType.equals("FacebookProfile")
		   || profileType.equals("Google2Profile")
		   || profileType.equals("LinkedIn2Profile")) {

	    final String[] alias = (String[])ATTRIBUTE_NAMES_LOOKUP.get(alaName);
	    for (int i = 0; i < alias.length; i++) {
		final Object match = userAttributes.get(alias[i]);
		if (match != null) {
		    return (String)match;
		}
	    }

	} else {
	    logger.debug("unknown profile type: {}", profileType);
	}

	// default return for no match found
	return null;
    }

    static final public String HTTP_GET(final String urlStr) {
	HttpURLConnection conn = null;
	BufferedReader reader = null;

	try {

	    final URL url = new URL(urlStr);
	    conn = (HttpURLConnection)url.openConnection();
	    conn.setRequestMethod("GET");

	    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    final StringBuffer result = new StringBuffer();

	    for (;;) {
		final String line = reader.readLine();
		if (line == null) {
		    break;
		}

		result.append(line);
	    }

	    return result.toString();

	} catch (Exception e) {
	    logger.debug("HTTP_GET error: {}", e.getMessage());

	} finally {
	    try {
		reader.close();
		conn.disconnect();

	    } catch (IOException ioe) {
		ioe.printStackTrace();
	    }
	}

	return null;
    }
}

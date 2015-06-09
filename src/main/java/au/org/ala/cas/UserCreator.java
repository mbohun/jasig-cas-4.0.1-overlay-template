package au.org.ala.cas;

import org.pac4j.core.profile.UserProfile;

/**
 * User creation interface. UserCreator takes user attributes received from
 * Facebook/Google/GitHub/etc. and uses those to setup and create an
 * application specific user in an application specific backend/storage
 * (no/sqlDB, LDAP, etc.)  
 *
 * @author Martin Bohun
 * @since 4.0.1
 */
public interface UserCreator {
    public void createUser(final UserProfile userProfile);
}

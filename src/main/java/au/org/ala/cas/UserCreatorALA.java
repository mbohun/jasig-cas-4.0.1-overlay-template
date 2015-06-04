package au.org.ala.cas;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

/**
 * UserCreatorALA takes user attributes received from Facebook/Google/etc.
 * and uses those to setup and create ALA user profile in the ALA
 * userdetails sql DB.
 *
 * @author Martin Bohun
 * @since 4.0.1
 */
public class UserCreatorALA implements UserCreator {

    /** Log instance. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /* TODO: add util userAttributes parser later; the parser will encapsulate and hide
     *       all the differences among the attribute formats received from Facebook,
     *       Google, GitHub, LinkedIn, etc.
     */
    @Override
    public void createUser(final Map userAttributes) {
	logger.debug("createUser: {}", userAttributes);
    }
}

package au.org.ala.cas;

import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.jasig.cas.authentication.handler.PasswordEncoder;
import org.jasig.cas.authentication.handler.PlainTextPasswordEncoder;

import au.org.ala.cas.AttributeParser;
import org.jasig.cas.support.pac4j.authentication.principal.ClientCredential;
import org.pac4j.core.profile.UserProfile;

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

    @NotNull
    private JdbcTemplate jdbcTemplate;

    @NotNull
    private DataSource dataSource;

    @NotNull
    private String sql;

    @NotNull
    private PasswordEncoder passwordEncoder = new PlainTextPasswordEncoder();

    @NotNull
    private String userCreatePassword;

    /* TODO: add util userAttributes parser later; the parser will encapsulate and hide
     *       all the differences among the attribute formats received from Facebook,
     *       Google, GitHub, LinkedIn, etc.
     */
    @Override
    public void createUser(final ClientCredential clientCredential) {
	final UserProfile userProfile = clientCredential.getUserProfile();
	logger.debug("createUser: {}", userProfile);

	final String email     = AttributeParser.lookup("email",     userProfile);
	if (email == null) {
	    return;
	}

	final String firstname = AttributeParser.lookup("firstname", userProfile);
	if (firstname == null) {
	    return;
	}

	final String lastname  = AttributeParser.lookup("lastname",  userProfile);
	if (lastname == null) {
	    return;
	}

	final String password = this.passwordEncoder.encode(this.userCreatePassword);

	final int rows_affected =
	    this.jdbcTemplate.update(this.sql,
				     email,                            //email
				     firstname,                        //firstname
				     lastname,                         //lastname
				     password,                         //password
				     "",                               //city
				     "",                               //organisation
				     "",                               //primaryUserType
				     "",                               //secondaryUserType
				     "N/A",                            //(AUS) state
				     "");                              //telephone

	logger.debug("createUser rows_affected: {}", rows_affected);
    }

    //TODO: refactor these from bean properties into ctor args

    /**
     * @param sql The sql to set.
     */
    public void setSql(final String sql) {
	this.sql = sql;
    }

    /**
     * Method to set the datasource and generate a JdbcTemplate.
     *
     * @param dataSource the datasource to use.
     */
    public final void setDataSource(final DataSource dataSource) {
	//TODO: move the JdbcTemplate creation into the ctor, i.e. make dataSource constructor-arg instead of property
	this.jdbcTemplate = new JdbcTemplate(dataSource);
	this.dataSource = dataSource;
    }

    /**
     * Sets the PasswordEncoder to be used with this class.
     *
     * @param passwordEncoder the PasswordEncoder to use when encoding
     * passwords.
     */
    public final void setPasswordEncoder(final PasswordEncoder passwordEncoder) {
	this.passwordEncoder = passwordEncoder;
    }

    /**
     * @param userCreatePassword The "default" password (segment) to set
     * for ALA user created/registered via Delegate Authentication.
     */
    public void setUserCreatePassword(final String userCreatePassword) {
	this.userCreatePassword = userCreatePassword;
    }
}

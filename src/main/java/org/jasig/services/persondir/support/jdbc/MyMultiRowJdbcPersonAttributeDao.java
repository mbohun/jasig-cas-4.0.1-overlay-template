package org.jasig.services.persondir.support.jdbc;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.jasig.services.persondir.IPersonAttributes;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Sub-class of {@link org.jasig.services.persondir.support.jdbc.MultiRowJdbcPersonAttributeDao} 
 * that uses an SQL query template without making assumptions about the existence of a where clause.
 * This permits a stored procedure to be passed as the SQL query for attribute lookup, e.g.
 * 
 * <p><code>call sp_get_user_attributes(?)</code></p>
 * 
 * @author peter.flemming@csiro.au
 */
public class MyMultiRowJdbcPersonAttributeDao extends MultiRowJdbcPersonAttributeDao {

    private final JdbcTemplate jdbcTemplate;
    private final String sql;
    
    public MyMultiRowJdbcPersonAttributeDao(DataSource ds, String sql) {
		super(ds, sql);
		this.jdbcTemplate = new JdbcTemplate(ds);
		this.sql = sql;
	}

    @Override
    protected List<IPersonAttributes> getPeopleForQuery(PartialWhereClause queryBuilder, String queryUserName) {
        
        //Execute the query
        List<Map<String, Object>> results = this.jdbcTemplate.queryForList(sql, queryUserName);
    	
       
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Executed '" + sql + "' with arguments " + queryBuilder.arguments + " and got results " + results);
        }

        return this.parseAttributeMapFromResults(results, queryUserName);
    }
}

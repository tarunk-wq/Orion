package org.dspace.content.dao;

import java.sql.SQLException;

import org.dspace.content.SourceToken;
import org.dspace.core.Context;

/**
 * DAO interface for SourceToken
 *
 * Handles database operations for the sourcetoken table
 */
public interface SourceTokenDAO {

    /**
     * Find SourceToken row using source name
     */
    SourceToken findBySource(Context context, String source) throws SQLException;

}
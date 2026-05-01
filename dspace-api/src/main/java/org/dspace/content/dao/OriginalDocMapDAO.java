package org.dspace.content.dao;

import org.dspace.core.Context;
import org.dspace.content.OriginalDocMap;

import java.sql.SQLException;

/*
 * DAO layer -> handles DB operations ONLY
 * (No business logic here)
 */
public interface OriginalDocMapDAO {

    void create(Context context, OriginalDocMap map) throws SQLException;
}
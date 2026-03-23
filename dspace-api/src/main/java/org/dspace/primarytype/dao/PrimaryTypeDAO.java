package org.dspace.primarytype.dao;

import java.sql.SQLException;
import org.dspace.core.Context;

public interface PrimaryTypeDAO {

    boolean existsByPrimaryType(Context context, String primaryType) throws SQLException;

}
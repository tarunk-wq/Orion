package org.dspace.item2agentagencypan.dao;

import java.sql.SQLException;
import org.dspace.core.Context;

public interface AgencyPanDAO {

    boolean existsByPan(Context context, String pan) throws SQLException;

}
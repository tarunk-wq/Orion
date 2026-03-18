package org.dspace.item2pan.dao;

import java.sql.SQLException;

import org.dspace.core.Context;

public interface PanDAO {

    boolean existsByPan(Context context, String pan) throws SQLException;

}
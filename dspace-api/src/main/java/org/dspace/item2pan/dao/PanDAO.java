package org.dspace.item2pan.dao;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.item2pan.Item2Pan;

public interface PanDAO {

    boolean existsByPan(Context context, String pan) throws SQLException;
    void create(Context context, String pan, Item item) throws SQLException;
    Item2Pan findByPan(Context context, String pan) throws SQLException;

}
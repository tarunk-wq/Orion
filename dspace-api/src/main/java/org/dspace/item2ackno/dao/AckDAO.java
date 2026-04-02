package org.dspace.item2ackno.dao;

import java.sql.SQLException;
import org.dspace.core.Context;
import org.dspace.item2ackno.Item2AckNo;
import org.dspace.content.Item;

public interface AckDAO {

    boolean existsByAckNo(Context context, String ackNo) throws SQLException;

    void create(Context context, String ackNo, Item item) throws SQLException;
    
    Item2AckNo findByAckNo(Context context, String ackNo) throws SQLException;
}
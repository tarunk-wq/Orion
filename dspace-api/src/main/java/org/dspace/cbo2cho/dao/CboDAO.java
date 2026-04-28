package org.dspace.cbo2cho.dao;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.cbo2cho.Cbo2Cho;

public interface CboDAO {

    boolean existsByCboNo(Context context, String cboNo) throws SQLException;

    void create(Context context, String cboNo, String cboName, String choNo) throws SQLException;

    Cbo2Cho findByCboNo(Context context, String cboNo) throws SQLException;
}
package org.dspace.cbo2cho.dao.impl;

import java.sql.SQLException;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.cbo2cho.Cbo2Cho;
import org.dspace.cbo2cho.dao.CboDAO;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

/**
 * Implementation of CboDAO using Hibernate
 */
@Repository
public class CboDAOImpl extends AbstractHibernateDAO<Cbo2Cho> implements CboDAO {

    @Override
    public boolean existsByCboNo(Context context, String cboNo) throws SQLException {

        String hql = "FROM Cbo2Cho WHERE lower(cboNo) = lower(:cboNo)";

        Query<Cbo2Cho> query = getHibernateSession(context)
                .createQuery(hql, Cbo2Cho.class);

        query.setParameter("cboNo", cboNo);

        return query.uniqueResult() != null;
    }

    @Override
    public void create(Context context, String cboNo, String cboName, String choNo)
            throws SQLException {

        Cbo2Cho entity = new Cbo2Cho();

        // Always set cbo_no
        entity.setCboNo(cboNo);

        // Only set cbo_name if not empty (matches legacy)
        if (cboName != null && !cboName.trim().isEmpty()) {
            entity.setCboName(cboName);
        }

        // Always set cho_no
        entity.setChoNo(choNo);

        // Save using Hibernate
        save(context, entity);
    }

    @Override
    public Cbo2Cho findByCboNo(Context context, String cboNo) throws SQLException {

        String hql = "FROM Cbo2Cho WHERE lower(cboNo) = lower(:cboNo)";

        Query<Cbo2Cho> query = getHibernateSession(context)
                .createQuery(hql, Cbo2Cho.class);

        query.setParameter("cboNo", cboNo);

        return query.uniqueResult();
    }
}
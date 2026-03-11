package org.dspace.eperson.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.PasswordHistory;

public interface PasswordHistoryDAO extends DSpaceObjectDAO<PasswordHistory>, DSpaceObjectLegacySupportDAO<PasswordHistory> {
	List<PasswordHistory> findByEPerson(Context context, EPerson eperson) throws SQLException;
}

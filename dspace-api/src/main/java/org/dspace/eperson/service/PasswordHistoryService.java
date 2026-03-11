package org.dspace.eperson.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.PasswordHistory;

public interface PasswordHistoryService {
	void addPasswordHistory(Context context, EPerson eperson, String password) throws SQLException;

    List<PasswordHistory> getPasswordHistories(Context context, EPerson eperson) throws SQLException;

    boolean isPasswordReused(Context context, EPerson eperson, String newPassword) throws SQLException;
}

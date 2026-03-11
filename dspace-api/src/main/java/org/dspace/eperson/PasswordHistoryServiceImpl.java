package org.dspace.eperson;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.eperson.dao.PasswordHistoryDAO;
import org.dspace.eperson.service.PasswordHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;

public class PasswordHistoryServiceImpl implements PasswordHistoryService{
	
	@Autowired
	private PasswordHistoryDAO passwordHistoryDAO;

	@Override
	public void addPasswordHistory(Context context, EPerson eperson, String password) throws SQLException {
		List<PasswordHistory> passwords = passwordHistoryDAO.findByEPerson(context, eperson);
		
		if(passwords.size() >= 3) {
			PasswordHistory oldestPass = passwords.get(passwords.size() - 1);
			passwordHistoryDAO.delete(context, oldestPass);
		}
		
		String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
		
		PasswordHistory newHistory = new PasswordHistory();
        newHistory.setEperson(eperson);
        newHistory.setPassword(hashedPassword);
        newHistory.setCreationDate(new Date());

        passwordHistoryDAO.create(context, newHistory);
	}

	@Override
	public List<PasswordHistory> getPasswordHistories(Context context, EPerson eperson) throws SQLException {
		 return passwordHistoryDAO.findByEPerson(context, eperson);
	}

	@Override
    public boolean isPasswordReused(Context context, EPerson eperson, String newPassword) throws SQLException {
        List<PasswordHistory> histories = passwordHistoryDAO.findByEPerson(context, eperson);
        return histories.stream()
                .anyMatch(h -> BCrypt.checkpw(newPassword, h.getPassword()));
    }

}

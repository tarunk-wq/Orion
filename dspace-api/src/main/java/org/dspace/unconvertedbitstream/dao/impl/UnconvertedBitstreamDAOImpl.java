package org.dspace.unconvertedbitstream.dao.impl;

import org.dspace.unconvertedbitstream.UnconvertedBitstream;
import org.dspace.unconvertedbitstream.dao.UnconvertedBitstreamDAO;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;

import java.sql.SQLException;

public class UnconvertedBitstreamDAOImpl extends AbstractHibernateDAO<UnconvertedBitstream>
		implements UnconvertedBitstreamDAO {

	@Override
	public UnconvertedBitstream create(Context context, UnconvertedBitstream entity) throws SQLException {

		return super.create(context, entity);
	}
}
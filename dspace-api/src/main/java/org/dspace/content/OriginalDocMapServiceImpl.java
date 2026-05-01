package org.dspace.content;

import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.content.OriginalDocMap;
import org.dspace.content.dao.OriginalDocMapDAO;
import org.dspace.content.service.OriginalDocMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

/*
 * Service Implementation
 * Equivalent to legacy:
 * InsertIntoOriginalDocMap(...)
 */
@Service
public class OriginalDocMapServiceImpl implements OriginalDocMapService {

	@Autowired
	private OriginalDocMapDAO originalDocMapDAO;

	@Override
	public void createMapping(Context context, Bitstream original, Bitstream converted) throws SQLException {

		/*
		 * Create new mapping object (Same as creating new row in legacy table)
		 */
		OriginalDocMap map = new OriginalDocMap();

		// Set original bitstream
		map.setOriginalBitstream(original);

		// Set converted bitstream
		map.setConvertedBitstream(converted);

		/*
		 * Save into DB via DAO (Same as DatabaseManager.insertNoSeq)
		 */
		originalDocMapDAO.create(context, map);
	}
}
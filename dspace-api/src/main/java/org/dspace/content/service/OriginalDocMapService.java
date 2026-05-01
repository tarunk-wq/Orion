package org.dspace.content.service;

import org.dspace.content.Bitstream;
import org.dspace.core.Context;

import java.sql.SQLException;

/*
 * Service layer -> business logic
 */
public interface OriginalDocMapService {

    void createMapping(Context context,
                       Bitstream original,
                       Bitstream converted) throws SQLException;
}
package org.dspace.unconvertedbitstream.dao;

import org.dspace.unconvertedbitstream.UnconvertedBitstream;
import org.dspace.core.Context;

import java.sql.SQLException;

public interface UnconvertedBitstreamDAO {

    UnconvertedBitstream create(Context context, UnconvertedBitstream entity) throws SQLException;
}
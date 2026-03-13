package org.dspace.content;

/**
 * 	replaces
 * 	Util.SUCCESS
	Util.FAILURE
	Util.UNAUTHORIZED
	from uploadbitstream in legacy code
 */
public enum UploadStatus {

    SUCCESS,
    INVALID_FILEFORMAT,
    FILE_FORMAT_MISSMATCH,
    FAILURE,
    NOTFOUND,
    UNAUTHORIZED,
    DATA_MISSING,
    INTERNAL_SERVER_ERROR
}
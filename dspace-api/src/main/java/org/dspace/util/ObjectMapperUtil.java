package org.dspace.util;

import java.io.IOException;
import java.io.InputStream;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ObjectMapperUtil {
	
	private static ObjectMapper mapper = new ObjectMapper();

	public static ObjectNode createObjectNode() {
		return mapper.createObjectNode();
	}
	
	public static ArrayNode createArrayNode() {
		return mapper.createArrayNode();
	}

	public static <T> T readValue(InputStream input, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
		return mapper.readValue(input, clazz);
	}
	
}

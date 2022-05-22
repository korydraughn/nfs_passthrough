package org.irods.nfs;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.dcache.nfs.status.BadOwnerException;
import org.dcache.nfs.v4.NfsIdMapping;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IpToUserMapper implements NfsIdMapping {
	
	private Map<String, String> ipToUserMappings;
	
	public IpToUserMapper(String ipToUserMappingsFilePath)
			throws JsonParseException, JsonMappingException, IOException {
		final var jsonMapper = new ObjectMapper();
		ipToUserMappings = jsonMapper.readValue(new File(ipToUserMappingsFilePath),
												new TypeReference<Map<String, String>>() {});
	}

	@Override
	public int principalToUid(String principal) throws BadOwnerException {
		return 0;
	}

	@Override
	public int principalToGid(String principal) throws BadOwnerException {
		return 0;
	}

	@Override
	public String uidToPrincipal(int id) {
		return null;
	}

	@Override
	public String gidToPrincipal(int id) {
		return null;
	}

}

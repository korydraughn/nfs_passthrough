package org.irods.nfs;

import org.dcache.nfs.status.BadOwnerException;
import org.dcache.nfs.v4.NfsIdMapping;

public class IdMapper implements NfsIdMapping
{
    @Override public int principalToUid(String principal) throws BadOwnerException
    {
        return 0;
    }

    @Override public int principalToGid(String principal) throws BadOwnerException
    {
        return 0;
    }

    @Override public String uidToPrincipal(int id)
    {
        return null;
    }

    @Override public String gidToPrincipal(int id)
    {
        return null;
    }
}

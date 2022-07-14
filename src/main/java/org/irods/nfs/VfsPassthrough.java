package org.irods.nfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.security.auth.Subject;

import org.dcache.nfs.status.NoEntException;
import org.dcache.nfs.v4.NfsIdMapping;
import org.dcache.nfs.v4.SimpleIdMap;
import org.dcache.nfs.v4.xdr.nfsace4;
import org.dcache.nfs.vfs.AclCheckable;
import org.dcache.nfs.vfs.DirectoryEntry;
import org.dcache.nfs.vfs.DirectoryStream;
import org.dcache.nfs.vfs.FsStat;
import org.dcache.nfs.vfs.Inode;
import org.dcache.nfs.vfs.Stat;
import org.dcache.nfs.vfs.Stat.Type;
import org.dcache.nfs.vfs.VirtualFileSystem;

import com.google.common.primitives.Longs;

public class VfsPassthrough implements VirtualFileSystem
{
    private final Path root;
    private final Map<Long, Path> inodeToPath = new ConcurrentHashMap<>();
    private final Map<Path, Long> pathToInode = new ConcurrentHashMap<>();
    private final AtomicLong fileId = new AtomicLong(1);
    private final NfsIdMapping idMapper = new SimpleIdMap();
    private final UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();

    public VfsPassthrough(String rootDir)
    {
        root = Path.of(rootDir);
    }

    @Override public int access(Subject subject, Inode inode, int mode) throws IOException
    {
        printLocation();
        return mode; // pseudofs will do the checks.
    }

    @Override public Inode create(Inode parent, Type type, String name, Subject subject, int mode) throws IOException
    {
        printLocation();
        // TODO Auto-generated method stub
        return null;
    }

    @Override public FsStat getFsStat() throws IOException
    {
        printLocation();
        final var store = Files.getFileStore(root);
        final var total = store.getTotalSpace();
        final var free = store.getUsableSpace();
        return new FsStat(total, Long.MAX_VALUE, total - free, pathToInode.size());
    }

    @Override public Inode getRootInode() throws IOException
    {
        printLocation();
        return toFileHandle(1);
    }

    @Override public Inode lookup(Inode parent, String name) throws IOException
    {
        printLocation();

        final var parentInodeNumber = getInodeNumber(parent);
        final var parentPath = resolveInode(parentInodeNumber);

        final var child = switch (name)
        {
            case "."  -> parentPath;
        	case ".." -> parentPath.getParent();
        	default   -> parentPath.resolve(name);
        };
        
        final var childInodeNumber = resolvePath(child);
        
        return toFileHandle(childInodeNumber);
    }

    @Override public Inode link(Inode parent, Inode link, String name, Subject subject) throws IOException
    {
        printLocation();
        // TODO Auto-generated method stub
        return null;
    }

    @Override public DirectoryStream list(Inode inode, byte[] verifier, long cookie) throws IOException
    {
        printLocation();
        
        final var inodeNumber = getInodeNumber(inode);
        final var path = resolveInode(inodeNumber);
        final var list = new ArrayList<DirectoryEntry>();
        
//        try (final var ds = Files.newDirectoryStream(path)) {
//        	var cook = 2;
//        	
//        	for (final var p : ds) {
//        		++cook;
//        		
//        		if (cook > 1) {
//        			final var ino = resolvePath(p);
//        			list.add(new DirectoryEntry(p.getFileName().toString(), toFileHandle(ino), statPath(p, ino), cook));
//        		}
//        	}
//        }
        
        return new DirectoryStream(list);
    }

    @Override public byte[] directoryVerifier(Inode inode) throws IOException
    {
        printLocation();
        return DirectoryStream.ZERO_VERIFIER;
    }

    @Override public Inode mkdir(Inode parent, String name, Subject subject, int mode) throws IOException
    {
        printLocation();
        // TODO Auto-generated method stub
        return null;
    }

    @Override public boolean move(Inode src, String oldName, Inode dest, String newName) throws IOException
    {
        printLocation();
        // TODO Auto-generated method stub
        return false;
    }

    @Override public Inode parentOf(Inode inode) throws IOException
    {
        printLocation();
        
        final var inodeNumber = getInodeNumber(inode);
        
        if (inodeNumber == 1) {
        	throw new NoEntException("No parent");
        }
        
        final var path = resolveInode(inodeNumber);
        final var parentPath = path.getParent();
        final var parentInodeNumber = resolvePath(parentPath);

        return toFileHandle(parentInodeNumber);
    }

    @Override public int read(Inode inode, byte[] data, long offset, int count) throws IOException
    {
        printLocation();
        // TODO Auto-generated method stub
        return 0;
    }

    @Override public String readlink(Inode inode) throws IOException
    {
        printLocation();
        final var inodeNumber = getInodeNumber(inode);
        final var path = resolveInode(inodeNumber);
        return Files.readSymbolicLink(path).toString();
    }

    @Override public void remove(Inode parent, String name) throws IOException
    {
        printLocation();
        // TODO Auto-generated method stub
    }

    @Override public Inode symlink(Inode parent, String name, String link, Subject subject, int mode) throws IOException
    {
        printLocation();
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WriteResult write(Inode inode, byte[] data, long offset, int count, StabilityLevel stabilityLevel)
        throws IOException
    {
        printLocation();
        
        final var inodeNumber = getInodeNumber(inode);
        final var path = resolveInode(inodeNumber);
        final var srcBuffer = ByteBuffer.wrap(data, 0, count);
        
        try (final var channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
        	final var bytesWritten = channel.write(srcBuffer, offset);
        	return new WriteResult(StabilityLevel.FILE_SYNC, bytesWritten);
        }
    }

    @Override public void commit(Inode inode, long offset, int count) throws IOException
    {
        printLocation();
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override public Stat getattr(Inode inode) throws IOException
    {
        printLocation();
        final var inodeNumber = getInodeNumber(inode);
        final var path = resolveInode(inodeNumber);
//        return statPath(path, inodeNumber);
        return null;
    }

    @Override public void setattr(Inode inode, Stat stat) throws IOException
    {
        printLocation();
        // TODO Auto-generated method stub
    }

    @Override public nfsace4[] getAcl(Inode inode) throws IOException
    {
        printLocation();
        return new nfsace4[0];
    }

    @Override public void setAcl(Inode inode, nfsace4[] acl) throws IOException
    {
        printLocation();
        // NOP
    }

    @Override public boolean hasIOLayout(Inode inode) throws IOException
    {
        printLocation();
        return false;
    }

    @Override public AclCheckable getAclCheckable()
    {
        printLocation();
        return AclCheckable.UNDEFINED_ALL;
    }

    @Override public NfsIdMapping getIdMapper()
    {
        return idMapper;
    }

    @Override public boolean getCaseInsensitive()
    {
        printLocation();
        return false;
    }

    @Override public boolean getCasePreserving()
    {
        printLocation();
        return false;
    }

    private Inode toFileHandle(long inodeNumber)
    {
        return Inode.forFile(Longs.toByteArray(inodeNumber));
    }
    
    private long getInodeNumber(Inode inode)
    {
    	return Longs.fromByteArray(inode.getFileId());
    }
    
    private Path resolveInode(Long inodeNumber) throws NoEntException
    {
    	final var path = inodeToPath.get(inodeNumber);

    	if (path == null) {
    		throw new NoEntException("inode #" + inodeNumber);
    	}
    	
    	return path;
    }
    
    private long resolvePath(Path path) throws NoEntException
    {
    	final var inodeNumber = pathToInode.get(path);
    	
    	if (inodeNumber == null) {
    		throw new NoEntException("path " + path);
    	}
    	
    	return inodeNumber;
    }
    
    private void map(long inodeNumber, Path path)
    {
    	if (inodeToPath.putIfAbsent(inodeNumber, path) != null) {
    		throw new IllegalStateException();
    	}
    	
    	final var otherInodeNumber = pathToInode.putIfAbsent(path, inodeNumber);
    	
    	if (otherInodeNumber != null) {
    		// Try to rollback.
    		if (inodeToPath.remove(inodeNumber) != path) {
    			throw new IllegalStateException("Can't map, rollback failed");
    		}
    		
    		throw new IllegalStateException("path " + path);
    	}
    }
    
    private void unmap(long inodeNumber, Path path)
    {
    	final var removedPath = inodeToPath.remove(inodeNumber);
    	
    	if (!path.equals(removedPath)) {
    		throw new IllegalStateException();
    	}
    	
    	if (pathToInode.remove(path) != inodeNumber) {
    		throw new IllegalStateException();
    	}
    }
    
    private void remap(long inodeNumber, Path oldPath, Path newPath)
    {
    	unmap(inodeNumber, oldPath);
    	map(inodeNumber, newPath);
    }

    private void printLocation()
    {
        final var methodName = new Throwable().getStackTrace()[1].getMethodName();
        System.out.println("In function: " + methodName);
    }
}

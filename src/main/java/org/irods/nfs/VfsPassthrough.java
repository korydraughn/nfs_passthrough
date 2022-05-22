package org.irods.nfs;

import java.io.IOException;

import javax.security.auth.Subject;

import org.dcache.nfs.v4.NFSv4StateHandler;
import org.dcache.nfs.v4.NfsIdMapping;
import org.dcache.nfs.v4.xdr.nfsace4;
import org.dcache.nfs.vfs.AclCheckable;
import org.dcache.nfs.vfs.DirectoryStream;
import org.dcache.nfs.vfs.FsStat;
import org.dcache.nfs.vfs.Inode;
import org.dcache.nfs.vfs.Stat;
import org.dcache.nfs.vfs.Stat.Type;
import org.dcache.nfs.vfs.VirtualFileSystem;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.primitives.Longs;

public class VfsPassthrough implements VirtualFileSystem {
	
	private NFSv4StateHandler nfsStateHandler;
	private IpToUserMapper idMapper;
	
	public VfsPassthrough(NFSv4StateHandler nfsStateHandler, String ipToUserMappingsFilePath)
			throws JsonParseException, JsonMappingException, IOException {
		this.nfsStateHandler = nfsStateHandler;
		this.idMapper = new IpToUserMapper(ipToUserMappingsFilePath);
	}

	@Override
	public int access(Subject subject, Inode inode, int mode) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Inode create(Inode parent, Type type, String name, Subject subject, int mode) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FsStat getFsStat() throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Inode getRootInode() throws IOException {
		printLocation();
		printClients();
		return toFileHandle(1);
	}

	@Override
	public Inode lookup(Inode parent, String name) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		printClients();
		return null;
	}

	@Override
	public Inode link(Inode parent, Inode link, String name, Subject subject) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DirectoryStream list(Inode inode, byte[] verifier, long cookie) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] directoryVerifier(Inode inode) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Inode mkdir(Inode parent, String name, Subject subject, int mode) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean move(Inode src, String oldName, Inode dest, String newName) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Inode parentOf(Inode inode) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int read(Inode inode, byte[] data, long offset, int count) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String readlink(Inode inode) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(Inode parent, String name) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		
	}

	@Override
	public Inode symlink(Inode parent, String name, String link, Subject subject, int mode) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WriteResult write(Inode inode, byte[] data, long offset, int count, StabilityLevel stabilityLevel)
			throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void commit(Inode inode, long offset, int count) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		
	}

	@Override
	public Stat getattr(Inode inode) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		printClients();
		return null;
	}

	@Override
	public void setattr(Inode inode, Stat stat) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		printClients();
		
	}

	@Override
	public nfsace4[] getAcl(Inode inode) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAcl(Inode inode, nfsace4[] acl) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasIOLayout(Inode inode) throws IOException {
		printLocation();
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AclCheckable getAclCheckable() {
		printLocation();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NfsIdMapping getIdMapper() {
		return idMapper;
	}

	@Override
	public boolean getCaseInsensitive() {
		printLocation();
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getCasePreserving() {
		printLocation();
		// TODO Auto-generated method stub
		return false;
	}
	
	private Inode toFileHandle(long inodeNumber) {
		return Inode.forFile(Longs.toByteArray(inodeNumber));
	}
	
	private void printClients() {
		System.out.println("Clients:");
		nfsStateHandler.getClients().forEach(c -> {
			System.out.println("\tIP => " + c.getRemoteAddress());
		});
	}
	
	private void printLocation() {
		final var methodName = new Throwable().getStackTrace()[1].getMethodName();
		System.out.println("In function: " + methodName);
	}

}

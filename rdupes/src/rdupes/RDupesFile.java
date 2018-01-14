package rdupes;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

import hu.qgears.commons.UtilFile;
import hu.qgears.commons.UtilMd5;

public class RDupesFile extends RDupesPath
{
	protected BasicFileAttributes attrs;
	private long size;
	private String hash;
	SizeCluster cl;
	private Object syncObject=new Object();
	public RDupesFile(RDupes rDupes, RDupesFolder parent, Path file, BasicFileAttributes attrs) {
		super(rDupes, parent, file);
		this.attrs=attrs;
		this.size=attrs.size();
		synchronized (syncObject) {
			cl=rDupes.createCluster(size);
			cl.addFile(this);
		}
	}
	/**
	 * TODO implement on N cores and with reused buffers.
	 * @return
	 */
	public String getHash() throws Exception {
		if(hash==null)
		{
			try(InputStream is=new FileInputStream(file.toFile())){
				MessageDigest m = MessageDigest.getInstance("MD5");
				byte[] buffer=new byte[UtilFile.defaultBufferSize.get()];
				int n=is.read(buffer, 0, buffer.length);
				while(n>0)
				{
					m.update(buffer, 0, n);
					n=is.read(buffer, 0, buffer.length);
				}
				hash=UtilMd5.toMd5String(m);
			}
		}
		return hash;
	}
	public Path getFile() {
		return file;
	}
	@SuppressWarnings("unchecked")
	@Override
	public List<RDupesObject> getChildren() {
		return Collections.EMPTY_LIST;
	}
	@Override
	protected void deleteChildren() {
		rd.createCluster(size).remove(this);
	}
	@Override
	protected boolean isFolder() {
		return false;
	}
	/**
	 * There is one more collision than before.
	 */
	public void markCollision() {
		hasCollision.setProperty(true);
	}
	/**
	 * There is one less colliding file but not zero yet.
	 */
	public void lessCollision() {
		// TODO Auto-generated method stub
		
	}
	/**
	 * There is one less collision than before
	 */
	public void unmarkCollision() {
		hasCollision.setProperty(false);
	}
	@Override
	public void modified() {
		rd.createCluster(size).remove(this);
		hasCollision.setProperty(false);
		hash=null;
		cl=null;
		try {
			attrs=Files.readAttributes(file, BasicFileAttributes.class);
			size=attrs.size();
			synchronized (syncObject) {
				cl=rd.createCluster(size);
			}
			cl.addFile(this);
		} catch(NoSuchFileException e)
		{
			// Removed while stuffing...
			delete(true);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public List<RDupesObject> getCollisions() {
		SizeCluster cl;
		synchronized (syncObject) {
			cl=this.cl;
		}
		if(cl!=null)
		{
			return cl.getCollisions(this);
		}else
		{
			return super.getCollisions();
		}
	}
}

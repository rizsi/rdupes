package rdupes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

public class RDupesFile extends RDupesPath
{
	private volatile LazyFileHash hash;
	SizeCluster cl;
	private int changeCounter=0;
	public String storedHash;
	public RDupesFile(RDupes rDupes, RDupesFolder parent, Path file, BasicFileAttributes attrs) {
		super(rDupes, parent, file);
		this.size=attrs.size();
		registerStatistics();
		synchronized (this) {
			cl=rDupes.createCluster(size);
			cl.addFile(this);
		}
	}
	public LazyFileHash getHash() {
		if(hash==null)
		{
			hash=new LazyFileHash(this, size);
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
		changeCounter++;
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
		setHasCollision(true);
	}
	/**
	 * There is one less colliding file but not zero yet.
	 */
	public void lessCollision() {
		// TODO Auto-generated method stub
		
	}
	/**
	 * Last collision is deleted
	 */
	public void unmarkCollision() {
		setHasCollision(false);
	}
	@Override
	public void modified() {
		changeCounter++;
		rd.createCluster(size).remove(this);
		deregisterStatistics();
		if(hash!=null)
		{
			hash.cancel();
			hash=null;
		}
		cl=null;
		try {
			BasicFileAttributes attrs=Files.readAttributes(file, BasicFileAttributes.class);
			size=attrs.size();
			synchronized (this) {
				cl=rd.createCluster(size);
			}
			cl.addFile(this);
			registerStatistics();
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
		synchronized (this) {
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
	public int getChangeCounter() {
		return changeCounter;
	}
	@Override
	public boolean hasChildren() {
		return false;
	}
	@Override
	public void delete(boolean removeFromParent) {
		super.delete(removeFromParent);
		deregisterStatistics();
	}
	protected void registerStatistics() {
		rd.filesProcessed.incrementAndGet();
		addChildNFile(1);
		getParent().addChildNFile(1);
		getParent().addChildSize(size);
	}

	protected void deregisterStatistics() {
		rd.filesProcessed.decrementAndGet();
		getParent().addChildNFile(-1);
		getParent().addChildSize(-size);
		setHasCollision(false);
	}
}

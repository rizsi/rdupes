package rdupes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class RDupesFile extends RDupesPath
{
	private volatile LazyFileHash hash;
	SizeCluster cl;
	private int changeCounter=0;
	public String storedHash;
	private HashSet<RDupesFile> collisions=new HashSet<>();
	private int farthestDupeLevel=Integer.MAX_VALUE;
	private long fileSize;
	public RDupesFile(RDupes rDupes, RDupesFolder parent, Path file, BasicFileAttributes attrs) {
		super(rDupes, parent, file);
		fileSize=attrs.size();
		registerStatistics();
		synchronized (this) {
			cl=rDupes.createCluster(fileSize);
			cl.addFile(this);
		}
	}
	public LazyFileHash getHash() {
		if(hash==null)
		{
			hash=new LazyFileHash(this, fileSize);
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
	protected boolean isFolder() {
		return false;
	}
	/**
	 * There is one more collision than before.
	 * @param file the colliding file
	 */
	public void addCollision(RDupesFile file) {
		boolean first=false;
		synchronized (this) {
			if(collisions==null)
			{
				collisions=new HashSet<>();
			}
			first=collisions.size()==0;
			collisions.add(file);
		}
		if(first)
		{
			addChildDupe(1);
			addChildDupeSize(fileSize);
		}
		updateNearestDepth();
		fireChange();
	}
	public void addCollisions(HashSet<RDupesFile> files) {
		boolean first=false;
		synchronized (this) {
			if(collisions==null)
			{
				collisions=new HashSet<>();
			}
			first=collisions.size()==0;
			collisions.addAll(files);
		}
		if(first)
		{
			addChildDupe(1);
			addChildDupeSize(fileSize);
		}
		updateNearestDepth();
		fireChange();
	}
	private void updateNearestDepth()
	{
		int depth=level;
		if(collisions!=null)
		{
			for(RDupesFile c: collisions)
			{
				int x=countFarthest(c);
				depth=Math.min(depth, x);
			}
		}
		setFarthestDupeDepth(depth);
	}
	
	private int countFarthest(RDupesFile dupe)
	{
		if(dupe.getRootFolder()==getRootFolder())
		{
			int l1=dupe.getLevel();
			int l0=getLevel();
			int checkLevel=Math.min(l1-1, l0-1);
			RDupesFolder f1=(RDupesFolder)dupe.getParent();
			RDupesFolder f0=(RDupesFolder)getParent();
			for(int i=1;i<l1-checkLevel;++i)
			{
				f1=(RDupesFolder)f1.getParent();
			}
			for(int i=1;i<l0-checkLevel;++i)
			{
				f0=(RDupesFolder)f0.getParent();
			}
			while(checkLevel>0)
			{
				if(f0==f1)
				{
					// System.out.println("Same origin: "+f0.getFullName()+" on level: "+checkLevel);
					return checkLevel;
				}
				checkLevel--;
				if(checkLevel>0)
				{
					f0=(RDupesFolder)f0.getParent();
					f1=(RDupesFolder)f1.getParent();
				}
			}
		}else
		{
			// System.out.println("in different tree! "+dupe.getFullName());
		}
		return 0;
	}
	
	protected void setFarthestDupeDepth(int depth)
	{
		if(depth!=farthestDupeLevel)
		{
			unregisterFarthestDupeDepth();
			addFarthestDupeLevel(depth);
			farthestDupeLevel=depth;
			fireChange();
		}
	}
	
	protected void unregisterFarthestDupeDepth()
	{
		if(farthestDupeLevel!=Integer.MAX_VALUE)
		{
			removeFarthestDupeLevel(farthestDupeLevel);
			fireChange();
		}
		farthestDupeLevel=Integer.MAX_VALUE;
	}

	/**
	 * There is one less colliding file but not zero yet.
	 * @param rDupesFile the collision removed
	 */
	public void lessCollision(RDupesFile rDupesFile) {
		boolean unmark=false;
		synchronized (this) {
			if(collisions!=null)
			{
				collisions.remove(rDupesFile);
				if(collisions.size()==0)
				{
					unmark=true;
				}
			}
		}
		if(unmark)
		{
			unmarkCollision(true);
		}
		updateNearestDepth();
	}
	/**
	 * Last collision is deleted
	 */
	public void unmarkCollision(boolean prev) {
		synchronized (this) {
			prev|=collisions!=null&&collisions.size()>0;
			collisions=null;
		}
		if(prev)
		{
			addChildDupe(-1);
			addChildDupeSize(-fileSize);
		}
	}
	@Override
	public void modified() {
		changeCounter++;
		rd.createCluster(fileSize).remove(this);
		deregisterStatistics();
		if(hash!=null)
		{
			hash.cancel();
			hash=null;
		}
		cl=null;
		try {
			BasicFileAttributes attrs=Files.readAttributes(file, BasicFileAttributes.class);
			fileSize=attrs.size();
			synchronized (this) {
				cl=rd.createCluster(fileSize);
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
		List<RDupesObject> ret=super.getCollisions();
		synchronized (this) {
			if(collisions!=null)
			{
				// TODO sort and cache list
				ret=new ArrayList<>(collisions);
			}
		}
		return ret;
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
		deregisterStatistics();
		changeCounter++;
		rd.createCluster(fileSize).remove(this);
		super.delete(removeFromParent);
	}
	protected void registerStatistics() {
		// System.out.println("Register: "+getFullName());
		rd.filesProcessed.incrementAndGet();
		addChildNFile(1);
		addChildSize(fileSize);
	}

	protected void deregisterStatistics() {
		// System.out.println("Deregister: "+getFullName());
		rd.filesProcessed.decrementAndGet();
		addChildNFile(-1);
		addChildSize(-fileSize);
		unregisterFarthestDupeDepth();
		unmarkCollision(false);
	}
	@Override
	public boolean hasCollision() {
		synchronized (this) {
			if(collisions!=null)
			{
				return !collisions.isEmpty();
			}
		}
		return false;
	}
	@Override
	protected void deleteChildren() {
	}
}

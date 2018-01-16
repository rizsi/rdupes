package rdupes;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import hu.qgears.commons.MultiMapHashToHashSetImpl;

public class SizeCluster implements IHashListener
{
	private long size;
	RDupes parent;
	public SizeCluster(RDupes parent, long size) {
		this.parent=parent;
		this.size=size;
	}
	private AtomicInteger nFile=new AtomicInteger(0);
	private RDupesFile singleFile;
	private MultiMapHashToHashSetImpl<String, RDupesFile> hashMap=null;
	public void addFile(RDupesFile rDupesFile) {
		int n=nFile.incrementAndGet();
		if(n==1)
		{
			singleFile=rDupesFile;
		}else if(n==2)
		{
			RDupesFile first=singleFile;
			singleFile=null;
			if(first!=null)
			{
				first.getHash().doWithHash(this);
			}
			rDupesFile.getHash().doWithHash(this);
		}else
		{
			rDupesFile.getHash().doWithHash(this);
		}
	}
	
	public void remove(RDupesFile rDupesFile) {
		int n=nFile.decrementAndGet();
		if(singleFile==rDupesFile)
		{
			singleFile=null;
		}
		if(hashMap!=null)
		{
			try {
				String hash = rDupesFile.storedHash;
				rDupesFile.storedHash=null;
				HashSet<RDupesFile> all=hashMap.getPossibleNull(hash);
				if(all!=null)
				{
					synchronized (this) {
						hashMap.removeSingle(hash, rDupesFile);
					}
					for(RDupesFile f: all)
					{
						f.lessCollision(rDupesFile);
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(hashMap.size()==1 && hashMap.values().iterator().next().size()==1)
			{
				RDupesFile single=hashMap.values().iterator().next().iterator().next();
				singleFile=single;
				synchronized (this) {
					hashMap=null;
				}
			}
		}
		if(n==0)
		{
			parent.removeSizeCluster(size, this);
		}
	}
	@Override
	public void hashCounted(RDupesFile hashReadyFile, String hash, int originalChangeCounter) {
		synchronized (parent.getSyncObject()) {
			synchronized (this) {
				if(originalChangeCounter==hashReadyFile.getChangeCounter())
				{
					if(hashMap==null)
					{
						hashMap=new MultiMapHashToHashSetImpl<>();
					}
					HashSet<RDupesFile> files=hashMap.get(hash);
					if(files.size()>0)
					{
						for(RDupesFile f: files)
						{
							f.addCollision(hashReadyFile);
						}
						hashReadyFile.addCollisions(files);
					}
					files.add(hashReadyFile);
					hashReadyFile.storedHash=hash;
				}
			}
		}
	}
//	public List<RDupesObject> getCollisions(RDupesFile rDupesFile) {
//		List<RDupesObject> colls=new ArrayList<>();
//		try {
//			synchronized (this) {
//				if(hashMap!=null)
//				{
//					HashSet<RDupesFile> ret=hashMap.get(rDupesFile.storedHash);
//					for(RDupesFile f: ret)
//					{
//						if(f!=rDupesFile)
//						{
//							colls.add(f);
//						}
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// TODO sort the returned list!
//		return colls;
//	}
}

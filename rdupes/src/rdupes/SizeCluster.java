package rdupes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import hu.qgears.commons.MultiMapHashToHashSetImpl;

public class SizeCluster {
	private long size;
	RDupes parent;
//	private Object syncObject=new Object();
	public SizeCluster(RDupes parent, long size) {
		this.parent=parent;
		this.size=size;
	}
	private RDupesFile singleFile;
	private MultiMapHashToHashSetImpl<String, RDupesFile> hashMap=null;
	public void addFile(RDupesFile rDupesFile) {
		if(hashMap==null)
		{
			if(singleFile==null)
			{
				//System.out.println("First file in size cluster: "+size+" "+rDupesFile.getFile());
				singleFile=rDupesFile;
			}else
			{
				hashMap=new MultiMapHashToHashSetImpl<>();
				insertByHash(singleFile);
				singleFile=null;
				insertByHash(rDupesFile);
			}
		}else
		{
			insertByHash(rDupesFile);
		}
	}
	private void insertByHash(RDupesFile singleFile) {
		String hash;
		try {
			hash = singleFile.getHash();
			HashSet<RDupesFile> files=hashMap.get(hash);
			files.add(singleFile);
			if(files.size()>1)
			{
				for(RDupesFile f: files)
				{
					f.markCollision();
				}
				//singleFile.markCollision();
			}
//			System.out.println("Multiple file in size cluster: "+size+" "+singleFile.getFile());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void printDupes() {
		if(hashMap!=null)
		{
			for(Map.Entry<String, Collection<RDupesFile>> entry: hashMap.entrySet())
			{
				Collection<RDupesFile> coll=entry.getValue();
				if(coll.size()>1)
				{
					for(RDupesFile f: coll)
					{
						System.out.println(""+f.getFile());
					}
					System.out.println("");
				}
			}
		}
	}
	public void remove(RDupesFile rDupesFile) {
		if(singleFile!=null)
		{
			if(singleFile==rDupesFile)
			{
				singleFile=null;
			}
		}
		if(hashMap!=null)
		{
			String hash;
			try {
				hash = rDupesFile.getHash();
				HashSet<RDupesFile> all=hashMap.get(hash);
				hashMap.removeSingle(hash, rDupesFile);
				if(all.size()==1)
				{
					all.iterator().next().unmarkCollision();
				}else
				{
					for(RDupesFile f: all)
					{
						f.lessCollision();
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
				hashMap=null;
			}
		}
		if(hashMap==null&&singleFile==null)
		{
			parent.removeSizeCluster(size, this);
		}
	}
	public List<RDupesObject> getCollisions(RDupesFile rDupesFile) {
		// TODO synchronize map access!
		List<RDupesObject> colls=new ArrayList<>();
		try {
			HashSet<RDupesFile> ret=hashMap.get(rDupesFile.getHash());
			for(RDupesFile f: ret)
			{
				if(f!=rDupesFile)
				{
					colls.add(f);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// TODO sort the returned list!
		return colls;
	}
}

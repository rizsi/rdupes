package rdupes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import hu.qgears.commons.UtilEventListener;

abstract public class RDupesObject {
	private int childDupes;
	private long size;
	protected long childDupesSize;
	protected int nFile=0;
	protected int level=0;
	private boolean allCopy=false;
	@SuppressWarnings("unchecked")
	private static UtilEventListener<RDupesObject>[] in=new UtilEventListener[1];
	private List<UtilEventListener<RDupesObject>> listeners=null;
	private UtilEventListener<RDupesObject>[]listenersCopy=null;
	public void addChangeListener(UtilEventListener<RDupesObject> l)
	{
		synchronized (this) {
			if(listeners==null)
			{
				listeners=new ArrayList<>();
			}
			listenersCopy=null;
			listeners.add(l);
		}
	}
	public void removeChangeListener(UtilEventListener<RDupesObject> l)
	{
		synchronized (this) {
			if(listeners==null)
			{
				return;
			}
			listenersCopy=null;
			listeners.remove(l);
			if(listeners.size()==0)
			{
				listeners=null;
			}
		}
	}
	protected void fireChange()
	{
		UtilEventListener<RDupesObject>[] ls;
		synchronized (this) {
			if(listenersCopy==null&&listeners!=null)
			{
				listenersCopy=listeners.toArray(in);
			}
			ls=listenersCopy;
		}
		if(ls!=null)
		{
			for(UtilEventListener<RDupesObject> o:ls)
			{
				o.eventHappened(this);
			}
		}
	}

	public RDupesObject() {
	}
	
	public void addChildDupe(int i) {
		childDupes+=i;
		updateAllCopy();
		fireChange();
		if(getParent()!=null)
		{
			getParent().addChildDupe(i);
		}
	}

	abstract public List<RDupesObject> getChildren();
	abstract public String getSimpleName();
	@SuppressWarnings("unchecked")
	public List<RDupesObject> getCollisions() {
		return Collections.EMPTY_LIST;
	}
	abstract public String getFullName();
	abstract public RDupesObject getParent();
	abstract public RDupes getHost();


	abstract public boolean hasChildren();
	public int getChildDupes() {
		return childDupes;
	}
	abstract public boolean hasCollision();
	public void addChildNFile(int i) {
		nFile+=i;
		updateAllCopy();
		if(getParent()!=null)
		{
			getParent().addChildNFile(i);
		}
		fireChange();
	}
	public void addChildSize(long csize) {
		size+=csize;
		if(getParent()!=null)
		{
			getParent().addChildSize(csize);
		}
		fireChange();
	}
	protected void addChildDupeSize(long l) {
		childDupesSize+=l;
		if(getParent()!=null)
		{
			getParent().addChildDupeSize(l);
		}
	}
	public String getStringInfo() {
		return "Size: "+RDupesStage.formatMemory(size)+" in "+nFile+" files. Duplicate size: "+RDupesStage.formatMemory(childDupesSize)+" in "+childDupes+" files. "+getFullName()+
				(allCopy?" Copy is within: "+getParentOnLevel(deepestLevel).getFullName():"");
	}
	public RDupesObject getParentOnLevel(int l)
	{
		RDupesObject o=this;
		while(o.getParent()!=null&&o.level>l)
		{
			o=o.getParent();
		}
		return o;
	}
	public long getChildDupesSize() {
		return childDupesSize;
	}
	public int getLevel() {
		return level;
	}
	private int[] levels;
	private volatile int deepestLevel=0;
	public void removeFarthestDupeLevel(int depth) {
		levels[depth]--;
		RDupesObject p=getParent();
		if(p!=null)
		{
			p.removeFarthestDupeLevel(depth);
		}
		if(depth<=deepestLevel)
		{
			int m=levels.length-1;
			for(;m>=0 && levels[m]==0;--m);
			if(m<deepestLevel)
			{
				deepestLevel=m;
				updateAllCopy();
				fireChange();
			}
		}
	}
	public void addFarthestDupeLevel(int depth) {
		if(levels==null)
		{
			levels=new int[depth+1];
		}else
		{
			int l=levels.length;
			while(l<=depth)
			{
				l*=2;
			}
			if(l!=levels.length)
			{
				levels=Arrays.copyOf(levels, l);
			}
		}
		levels[depth]++;
		RDupesObject p=getParent();
		if(p!=null)
		{
			p.addFarthestDupeLevel(depth);
		}
		if(deepestLevel<depth)
		{
			deepestLevel=depth;
			updateAllCopy();
			fireChange();
		}
	}
	private void updateAllCopy() {
		setAllCopy(deepestLevel<level&&childDupes==nFile);
	}
	private void setAllCopy(boolean b) {
		if(allCopy!=b)
		{
			allCopy=b;
			fireChange();
		}
	}
	public int getDeepestLevel() {
		return deepestLevel;
	}
	public boolean isAllCopy() {
		return allCopy;
	}
	public long getChildSize()
	{
		return size;
	}
}

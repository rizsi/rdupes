package rdupes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hu.qgears.commons.UtilEventListener;

abstract public class RDupesObject {
	private int childDupes;
	protected boolean collision; 
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
	
	protected void setHasCollision(boolean b) {
		if(collision!=b)
		{
			collision=b;
			fireChange();
			getParent().addChildDupe(b?1:-1);
		}
	}
	public void addChildDupe(int i) {
		childDupes+=i;
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
	public boolean hasCollision() {
		return collision;
	}
}

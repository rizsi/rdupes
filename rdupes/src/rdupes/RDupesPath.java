package rdupes;

import java.nio.file.Path;

abstract public class RDupesPath extends RDupesObject {
	protected RDupes rd;
	public RDupesFolder parent;
	final public Path file;
	/**
	 * File name override - used for root folder only.
	 */
	private String simpleName;
	protected RDupesFolder rootFolder;


	public RDupesPath(RDupes rd, RDupesFolder parent, Path file) {
		super();
		this.rd=rd;
		this.parent=parent;
		this.file = file;
		if(this.parent!=null)
		{
			this.parent.add(this);
		}
		level=getParent().getLevel()+1;
		if(parent!=null)
		{
			rootFolder=parent.getRootFolder();
		}else
		{
			rootFolder=(RDupesFolder)this;
		}
	}
	public RDupesFolder getRootFolder() {
		return rootFolder;
	}
	public String toString() {
		return file==null?"null":
			(parent==null?file.toString():file.getFileName().toString())+(isFolder()?"/":"");
	};
	@Override
	public String getFullName() {
		return (file==null?"null":
			file.toString())+(isFolder()?"/":"");
	}
	protected abstract boolean isFolder();
	@Override
	public String getSimpleName() {
		return simpleName==null?(file==null?"null":file.getFileName().toString()):simpleName;
	}
	public void delete(boolean removeFromParent) {
		deleteChildren();
		if(getParent()!=null)
		{
			if(removeFromParent)
			{
				getParent().remove(this);
			}
			parent=null;
		}
	}
	protected abstract void deleteChildren();
	abstract public void modified();
	@Override
	public RDupesObject getParent() {
		if(parent!=null)
		{
			return parent;
		}else
		{
			return rd;
		}
	}
	@Override
	public RDupes getHost() {
		return rd;
	}
	public void setSimpleName(String simpleName) {
		this.simpleName = simpleName;
	}
	/**
	 * Get the local name of this path.
	 * Local name starts from the root RDupes folder and is concatenated on demand.
	 * Name by setSimpleName() of local root folder is used. 
	 * @return
	 */
	public String getLocalName()
	{
		StringBuilder ret=new StringBuilder();
		appendLocalName(ret);
		return ret.toString();
	}
	protected void appendLocalName(StringBuilder ret) {
		if(parent!=null)
		{
			parent.appendLocalName(ret);
			ret.append("/");
		}
		ret.append(getSimpleName());
	}
}



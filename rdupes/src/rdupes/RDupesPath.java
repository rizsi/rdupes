package rdupes;

import java.nio.file.Path;

abstract public class RDupesPath extends RDupesObject {
	protected RDupes rd;
	protected RDupesFolder parent;
	protected Path file;

	public RDupesPath(RDupes rd, RDupesFolder parent, Path file) {
		super();
		this.rd=rd;
		this.parent=parent;
		this.file = file;
		if(this.parent!=null)
		{
			this.parent.add(this);
		}
		if(file!=null)
		{
			rd.pathMap.put(file, this);
		}
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
		return file==null?"null":file.getFileName().toString();
	}
	public void delete(boolean removeFromParent) {
		deleteChildren();
		if(file!=null)
		{
			rd.pathMap.remove(file, this);
		}
		if(parent!=null)
		{
			parent.remove(this);
		}
	}
	protected abstract void deleteChildren();
	abstract public void modified();
}

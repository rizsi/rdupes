package rdupes;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.ignore.IgnoreNode.MatchResult;

public class RDupesFolder extends RDupesPath {
	protected Map<String, RDupesPath> files=new TreeMap<>();
	private IgnoreNode ignore;
	private File trashDir;
	/**
	 * Relative path to the root folder.
	 */
	private String relativePath;
	public RDupesFolder(RDupes rd, RDupesFolder parent, Path file) {
		super(rd, parent, file);
		rd.foldersProcessed.incrementAndGet();
	}
	
	public void add(RDupesPath rDupesPath) {
		synchronized (files) {
			files.put(rDupesPath.file.getFileName().toString(), rDupesPath);
		}
		fireChange();
	}

	@Override
	public List<RDupesObject> getChildren() {
		synchronized (files) {
			return new ArrayList<>(files.values());
		}
	}

	public RDupesPath get(String string) {
		return files.get(string);
	}

	public void remove(RDupesPath rDupesPath) {
		files.remove(rDupesPath.getSimpleName());
		fireChange();
	}

	@Override
	protected void deleteChildren() {
		for(RDupesPath p: files.values())
		{
			p.delete(false);
		}
		files.clear();
	}
	
	@Override
	public void delete(boolean removeFromParent) {
		super.delete(removeFromParent);
		rd.foldersProcessed.decrementAndGet();
	}

	@Override
	protected boolean isFolder() {
		return true;
	}

	@Override
	public void modified() {
	}
	public void setIgnoreFile(IgnoreNode in) {
		this.ignore=in;
	}

	/**
	 * 
	 * @param dir
	 * @param entryPath relative to the root folder path
	 * @param negateFirstMatch
	 * @param isDirectory
	 * @return
	 */
	public boolean isIgnored(Path dir, String entryPath, boolean negateFirstMatch, boolean isDirectory) {
		MatchResult res=MatchResult.CHECK_PARENT;
		if(ignore!=null)
		{
			if(relativePath==null)
			{
				relativePath=getRootFolder().file.relativize(file).toString();
			}
			String localPath=entryPath.substring(relativePath.length());
			res=ignore.isIgnored(localPath, isDirectory);
		}
		switch(res)
		{
		case CHECK_PARENT:
			if(parent!=null)
			{
				return parent.isIgnored(dir, entryPath, false, isDirectory);
			}
			return false;
		case CHECK_PARENT_NEGATE_FIRST_MATCH:
			if(parent!=null)
			{
				return parent.isIgnored(dir, entryPath, true, isDirectory);
			}
			return false;
		case IGNORED:
			return true;
		case NOT_IGNORED:
			return false;
		default:
			throw new RuntimeException("Unhandled case: "+res);
		}
	}
	@Override
	public boolean hasChildren() {
		synchronized (files) {
			return !files.isEmpty();
		}
	}

	public void setTrashDir(File trashDir) {
		this.trashDir=trashDir;
		fireChange();
	}
	public File getTrashDir() {
		return trashDir;
	}

	@Override
	public boolean hasCollision() {
		return false;
	}
}

package rdupes;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RDupesFolder extends RDupesPath {
	protected Map<String, RDupesPath> files=new TreeMap<>();
	public RDupesFolder(RDupes rd, RDupesFolder parent, Path file) {
		super(rd, parent, file);
	}

	public void add(RDupesPath rDupesPath) {
		synchronized (files) {
			files.put(rDupesPath.file.getFileName().toString(), rDupesPath);
		}
		changed.eventHappened(this);
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
		changed.eventHappened(this);
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
	protected boolean isFolder() {
		return true;
	}

	@Override
	public void modified() {
	}

}

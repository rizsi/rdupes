package rdupes;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import hu.qgears.commons.signal.SignalFutureWrapper;

public class RDupes extends RDupesObject {
	public static void main(String[] args) throws Exception {
		List<Path> l=new ArrayList<>();
		for(String s: args)
		{
			Path p=Paths.get(s);
			l.add(p);
		}
		new RDupes().run(true, l);
	}
	
	private List<RDupesFolder> roots=new ArrayList<>();
	private HashMap<WatchKey, RDupesFolder> paths=new HashMap<>();
	/**
	 * Maps the size of the file to a container where all same sized files are.
	 */
	private Map<Long, SizeCluster> sizeMap=new TreeMap<>();
	/**
	 * Maps the path to the file.
	 * TODO remove!
	 */
	protected Map<Path, RDupesPath> pathMap=new HashMap<>();

	private WatchService ws;
	
	private class FVisitor extends SimpleFileVisitor<Path>
	{
		private RDupesFolder currentFolder;

		public FVisitor(RDupesFolder currentFolder) {
			super();
			this.currentFolder = currentFolder;
		}
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if(dir.getFileName().startsWith(".git"))
			{
				return FileVisitResult.SKIP_SUBTREE;
			}
			if(dir.getFileName().startsWith(".svn"))
			{
				return FileVisitResult.SKIP_SUBTREE;
			}
			if(Files.isSymbolicLink(dir))
			{
				return FileVisitResult.SKIP_SUBTREE;
			}
			
			RDupesFolder newFolder=new RDupesFolder(RDupes.this, currentFolder, dir);
			if(currentFolder==null)
			{
				synchronized (roots) {
					roots.add(newFolder);
				}
				changed.eventHappened(RDupes.this);
			}
			currentFolder=newFolder;
			// System.out.println("registering " + dir + " in watcher service");
			WatchKey watchKey = dir.register(ws, new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY });
			addKey(watchKey, currentFolder);
			return FileVisitResult.CONTINUE;
		}
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if(currentFolder!=null && currentFolder.file!=null && currentFolder.file.equals(dir))
			{
				currentFolder=currentFolder.parent;
			}
			return super.postVisitDirectory(dir, exc);
		}
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if(attrs.isRegularFile())
			{
				RDupesFile f=new RDupesFile(RDupes.this, currentFolder, file, attrs);
			}
			return super.visitFile(file, attrs);
		}
	}
	private void registerRecursive(RDupesFolder root, Path file) {
		File f=file.toFile();
		if (!f.exists()) {
			return;
		}
		if(Files.isSymbolicLink(file))
		{
			return;
		}
		try {
			Files.walkFileTree(file, new FVisitor(root));
		} catch (IOException e) {
			throw new RuntimeException("Error registering path " + file);
		}
	}

	private void addKey(WatchKey watchKey, RDupesFolder dir) {
		paths.put(watchKey, dir);
	}
	public final SignalFutureWrapper<RDupes> initializeDone=new SignalFutureWrapper<>();

	public void run(boolean listen, List<Path> initialPaths) throws Exception {
		FileSystem fs = FileSystems.getDefault();
		ws = fs.newWatchService();
		for(Path p:initialPaths)
		{
			registerRecursive(null, p);
		}
		initializeDone.ready(this, null);
		printDupes();
		while (listen) {
			WatchKey key;
			try {
				// wait for a key to be available
				key = ws.take();
			} catch (InterruptedException ex) {
				return;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				// get event type
				WatchEvent.Kind<?> kind = event.kind();

				// get file name
				@SuppressWarnings("unchecked")
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path fileName = ev.context();

				RDupesFolder folder=paths.get(key);
				Path fullp=folder.file.resolve(fileName);
				System.out.println(kind.name() + ": " + fullp);
				
				RDupesPath p=folder.get(fileName.toString());
				if (kind == StandardWatchEventKinds.OVERFLOW) {
					System.err.println("Overflow!!!");
					continue;
				} else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
					if(p!=null)
					{
						p.delete(true);
					}
					registerRecursive(folder, fullp);
					// process create event

				} else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
					if(p!=null)
					{
						p.delete(true);
					}
					// process delete event

				} else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
					if(p!=null)
					{
						p.modified();
					}
					// process modify event

				}
			}

			// IMPORTANT: The key must be reset after processed
			boolean valid = key.reset();
			if (!valid) {
				paths.remove(key);
				System.err.println("Remaining keys: "+paths.size());
				// break;
			}
		}
	}

	private void printDupes() {
		for(Map.Entry<Long, SizeCluster> entry: sizeMap.entrySet())
		{
			SizeCluster sc=entry.getValue();
			sc.printDupes();
		}
	}

	public SizeCluster createCluster(long size) {
		SizeCluster sc=sizeMap.get(size);
		if(sc==null)
		{
			sc=new SizeCluster(this, size);
			sizeMap.put(size, sc);
		}
		return sc;
	}

	@Override
	public List<RDupesObject> getChildren() {
		synchronized (roots) {
			return new ArrayList<>(roots);
		}
	}

	public void start(List<Path> l) {
		Thread t=new Thread(RDupes.class.getSimpleName()){
			@Override
			public void run() {
				try {
					RDupes.this.run(true, l);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	@Override
	public String getSimpleName() {
		return RDupes.class.getSimpleName();
	}
	@Override
	public String getFullName() {
		return RDupes.class.getSimpleName();
	}

	public void removeSizeCluster(long size, SizeCluster sizeCluster) {
		sizeMap.remove(size, sizeCluster);
	}
}

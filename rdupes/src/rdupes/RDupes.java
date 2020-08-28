package rdupes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jgit.ignore.IgnoreNode;

public class RDupes extends RDupesObject {
	public static final String IGNORE_FILE_NAME = ".rdupesignore";
	private static DecimalFormat df = new DecimalFormat("##.###");
	private RDupesClient client=new RDupesClient() {};

	public static void main(String[] args) throws Exception {
		RDupes rd=new RDupes();
		Map<String,Path> l=new TreeMap<>();
		for(String s: args)
		{
			Path p=Paths.get(s);
			l.put(rd.nextFolderName(), p);
		}
		rd.run(true, l, 1);
	}
	
	public final AtomicInteger foldersProcessed=new AtomicInteger(0);
	public final AtomicInteger filesProcessed=new AtomicInteger(0);
	public final AtomicInteger nFileToHash=new AtomicInteger(0);
	public final AtomicLong nBytesToHahs=new AtomicLong(0);
	/**
	 * When this is zero then the state is up to date.
	 */
	public final AtomicInteger tasks=new AtomicInteger(1);
	private Object globalLock=new Object();
	private RDupesArgs args=new RDupesArgs();
	public HashingExecutor hashing=new HashingExecutor();

	
	private List<RDupesFolder> roots=new ArrayList<>();
	private HashMap<WatchKey, RDupesFolder> paths=new HashMap<>();
	/**
	 * Maps the size of the file to a container where all same sized files are.
	 */
	private Map<Long, SizeCluster> sizeMap=new TreeMap<>();

	private WatchService ws;
	/**
	 * Root folder name auto index - should be used when folder name is not interesting.
	 */
	private int folderNameIndex;
	
	private class FVisitor extends SimpleFileVisitor<Path>
	{
		private RDupesFolder currentFolder;
		private String folderName;
		private boolean skipCreateRootFolder;

		public FVisitor(RDupesFolder currentFolder, String folderName) {
			super();
			this.currentFolder = currentFolder;
			this.folderName=folderName;
			skipCreateRootFolder=(folderName==null);
		}
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if(skipCreateRootFolder)
			{
				skipCreateRootFolder=false;
				return FileVisitResult.CONTINUE;
			}
			if(isIgnored(dir, true))
			{
				return FileVisitResult.SKIP_SUBTREE;
			}
			if(Files.isSymbolicLink(dir))
			{
				return FileVisitResult.SKIP_SUBTREE;
			}
			RDupesFolder newFolder=new RDupesFolder(RDupes.this, currentFolder, dir);
			if(folderName!=null)
			{
				newFolder.setSimpleName(folderName);
				folderName=null;
			}
			if(currentFolder==null)
			{
				synchronized (roots) {
					roots.add(newFolder);
				}
				fireChange();
			}
			if(args.isIgnoreFilesAllowed())
			{
				Path file=dir.resolve(IGNORE_FILE_NAME);
				if(file.toFile().isFile())
				{
					updateIgnoreFile(newFolder, file, false);
				}
			}
			currentFolder=newFolder;
			WatchKey watchKey = dir.register(ws, new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY });
			addKey(watchKey, currentFolder);
			return FileVisitResult.CONTINUE;
		}
		public boolean isIgnored(Path dir, boolean isFolder) {
			if(currentFolder!=null)
			{
				String relPath=currentFolder.getRootFolder().file.relativize(dir).toString();
				boolean ignored=currentFolder.isIgnored(dir, relPath, false, isFolder);
				return ignored;
			}
			return false;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if(currentFolder!=null && currentFolder.file!=null && currentFolder.file.equals(dir))
			{
				currentFolder=currentFolder.parent;
			}
			if(exc!=null)
			{
				System.err.println("Folder is deleted while visiting: "+exc.getMessage());
			}
			return FileVisitResult.CONTINUE;
		}
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if(args.isIgnoreFilesAllowed()&&file.getFileName().toString().equals(IGNORE_FILE_NAME))
			{
				// Ignore files are not processed here but when opening the folder
			}else
			{
				if(attrs.isRegularFile())
				{
					if(!isIgnored(file, false))
					{
						RDupesFile f=new RDupesFile(RDupes.this, currentFolder, file, attrs);
						client.fileVisited(f);
					}
				}
			}
			return super.visitFile(file, attrs);
		}
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			if(exc!=null)
			{
				System.err.println("File is deleted while visiting: "+exc.getMessage());
			}
			return FileVisitResult.CONTINUE;
		}
	}
	synchronized public String nextFolderName() {
		return ""+(folderNameIndex++);
	}
	public RDupes setClient(RDupesClient client) {
		this.client = client;
		return this;
	}
	public RDupesClient getClient() {
		return client;
	}
	/**
	 * 
	 * @param folder
	 * @param file
	 * @param modify true means that it is called from a modify event. This means that all children nodes of the folder must be re-examined!
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void updateIgnoreFile(RDupesFolder folder, Path file, boolean modify) throws FileNotFoundException, IOException
	{
		if(file!=null)
		{
			try(FileInputStream fis=new FileInputStream(file.toFile()))
			{
				IgnoreNode in=new IgnoreNode();
				in.parse(fis);
				folder.setIgnoreFile(in);
			}
		}else
		{
			folder.setIgnoreFile(null);
		}
		if(modify)
		{
			reloadFolder(folder);
		}
	}
	/**
	 * TODO optimize
	 *  * Do not reload everything!
	 *  * Only delete subtrees that are now ignored
	 *  * Only add subtrees that are not not ignored
	 * @param folder
	 * @throws IOException
	 */
	private void reloadFolder(RDupesFolder folder) throws IOException {
		// Delete all children and then re-visit the folder!
		folder.deleteChildren();
		Files.walkFileTree(folder.file, new FVisitor(folder, null));
	}
	private void registerRecursive(RDupesFolder root, String folderName, Path file) {
		File f=file.toFile();
		if (!f.exists()) {
			return;
		}
		if(Files.isSymbolicLink(file))
		{
			return;
		}
		try {
			Files.walkFileTree(file, new FVisitor(root, folderName));
		} catch (IOException e) {
			throw new RuntimeException("Error registering path " + file, e);
		}
	}

	private void addKey(WatchKey watchKey, RDupesFolder dir) {
		paths.put(watchKey, dir);
	}

	public void run(boolean listen, Map<String, Path> initialPaths, int nHashThread) throws Exception {
		FileSystem fs = FileSystems.getDefault();
		ws = fs.newWatchService();
		for(Map.Entry<String, Path> p:initialPaths.entrySet())
		{
			synchronized(globalLock)
			{
				registerRecursive(null, p.getKey(), p.getValue());
			}
		}
		hashing.setNThread(nHashThread);
		while (listen) {
			executeOneIteration();
		}
	}

	private void executeOneIteration() throws FileNotFoundException, IOException, InterruptedException {
		WatchKey key;
		try {
			tasks.decrementAndGet();
			// wait for a key to be available
			key = ws.take();
			tasks.incrementAndGet();
		} catch (ClosedWatchServiceException|InterruptedException ex) {
			// If this happens then this thread was stopped - no need to log the exception.
			return;
		}
		processUpdates(key);
	}

	private void processUpdates(WatchKey key) {
		synchronized(globalLock)
		{
			RDupesFolder folder=paths.get(key);
			for (WatchEvent<?> event : key.pollEvents()) {
				// get event type
				WatchEvent.Kind<?> kind = event.kind();

				// get file name
				@SuppressWarnings("unchecked")
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				try
				{
					Path fileName = ev.context();
	
					Path fullp=folder.file.resolve(fileName);
					// System.out.println(kind.name() + ": " + fullp);
					
					RDupesPath p=folder.get(fileName.toString());
					if (kind == StandardWatchEventKinds.OVERFLOW) {
						System.err.println("Overflow!!!");
						continue;
					} else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
						if(p!=null)
						{
							p.delete(true);
						}
						if(fileName.getFileName().toString().equals(IGNORE_FILE_NAME))
						{
							updateIgnoreFile(folder, fullp, true);
						}else
						{
							registerRecursive(folder, null, fullp);
						}
						// process create event
					} else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
						if(p!=null)
						{
							p.delete(true);
						}
						if(fileName.getFileName().toString().equals(IGNORE_FILE_NAME))
						{
							updateIgnoreFile(folder, null, true);
						}
						// process delete event
					} else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
						if(p!=null)
						{
							p.modified();
							client.fileModified(p);
						}else
						{
							if(fileName.getFileName().toString().equals(IGNORE_FILE_NAME))
							{
								updateIgnoreFile(folder, fullp, true);
							}
						}
						// process modify event
					}
				}catch (Exception e) {
					StringBuilder errStr=new StringBuilder("Error processing event: ");
					try {
						errStr.append(ev.kind().name());
					} catch (Exception e1) {
					}
					errStr.append(" fullname: ");
					try {
						errStr.append(folder.getFullName());
					} catch (Exception e1) {
					}
					errStr.append(" context: ");
					try {
						errStr.append(ev.context());
					} catch (Exception e1) {
					}
					System.err.println(errStr.toString());
					e.printStackTrace();
				}
			}
			// IMPORTANT: The key must be reset after processed
			boolean valid = key.reset();
			if (!valid) {
				if(folder.isRootFolder())
				{
					// If folder is not root then the parent will be notified to delete it. Otherwise it has to be deleted manually here.
					folder.delete(true);
				}
				paths.remove(key);
			}
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

	public void start(int nHashThread, List<Path> l) {
		Map<String, Path> m=new TreeMap<>();
		for(Path p: l)
		{
			m.put(nextFolderName(), p);
		}
		start(nHashThread, m);
	}
	public void start(int nHashThread, Map<String, Path> l) {
		Thread t=new Thread(RDupes.class.getSimpleName()){
			@Override
			public void run() {
				try {
					RDupes.this.run(true, l, nHashThread);
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

	public void addFolder(String folderName, File selected) {
		new Thread("RDupes add folder")
		{
			public void run() {
				tasks.incrementAndGet();
				synchronized (globalLock) {
					registerRecursive(null, folderName, Paths.get(selected.getAbsolutePath()));
				}
				tasks.decrementAndGet();
			};
		}.start();
	}

	@Override
	public RDupesObject getParent() {
		return null;
	}
	@Override
	public RDupes getHost() {
		return this;
	}
	public void removeRootFolderFromModel(RDupesFolder item) {
		tasks.incrementAndGet();
		synchronized (globalLock) {
			item.delete(true);
			roots.remove(item);
		}
		tasks.decrementAndGet();
		fireChange();
	}
	public Object getSyncObject() {
		return globalLock;
	}
	@Override
	public boolean hasChildren() {
		synchronized (globalLock) {
			return !roots.isEmpty();
		}
	}
	public void setNCores(int parseInt) {
		hashing.setNThread(parseInt);
	}
	@Override
	public String toString() {
		return "RDupes - drag folders to find duplicates within.";
	}
	@Override
	public boolean hasCollision() {
		return false;
	}
	@Override
	public void remove(RDupesPath rDupesPath)
	{
		roots.remove(rDupesPath);
		fireChange();
	}
	public static String formatMemory(long m) {
		long gig = 1024l * 1024l * 1024l;
		long meg = 1024l * 1024l;
		long kilo = 1024l;
		if (m >= gig) {
			double v = ((double) m) / gig;
			synchronized (df) {
				return df.format(v) + "Gib";
			}
		}
		if (m >= meg) {
			double v = ((double) m) / meg;
			synchronized (df) {
				return df.format(v) + "Mib";
			}
		}
		if (m >= kilo) {
			double v = ((double) m) / kilo;
			synchronized (df) {
				return df.format(v) + "Kib";
			}
		}
		return "" + m + " bytes";
	}
	public IHashProvider startHash(RDupesFile rDupesFile, long fileSize, long lastModified) {
		return client.startHash(rDupesFile, fileSize, lastModified);
	}
}

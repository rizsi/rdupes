package rdupes;

/**
 * When RDupes is used as a file system listener then this 
 * interface can be used to connect to a client.
 */
public interface RDupesClient {
	/**
	 * A file was added to the RDupes data structure.
	 * @param f
	 */
	default void fileVisited(RDupesFile f) {}

	default IHashProvider startHash(RDupesFile rDupesFile, long fileSize, long lastModified) {
		LazyFileHash ret=new LazyFileHash(rDupesFile, fileSize);
		ret.executeHashing();
		return ret;
	}

}

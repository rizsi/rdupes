package rdupes;

public interface IHashListener {
	void hashCounted(RDupesFile f, String hash, int originalChangeCounter);
}

package rdupes;

public interface IHashProvider {

	public void doWithHash(IHashListener sizeCluster);

	public void cancel();
}

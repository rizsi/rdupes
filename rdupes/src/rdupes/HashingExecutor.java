package rdupes;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import hu.qgears.commons.UtilFile;

/**
 * Execute crypt hashing (md5 or similar) of files in parallel manner.
 */
public class HashingExecutor {
	private LinkedBlockingQueue<LazyFileHash> tasks=new LinkedBlockingQueue<>();
	class HT extends Thread
	{
		public HT(String name) {
			super(name);
		}
		volatile boolean exit;
		@Override
		public void run() {
			try {
				MessageDigest m = MessageDigest.getInstance("MD5");
				ByteBuffer bb=ByteBuffer.allocateDirect(UtilFile.defaultBufferSize.get()*8);
				while(!exit)
				{
					LazyFileHash t;
					try {
						t = tasks.poll(10000, TimeUnit.MILLISECONDS);
						if(t!=null)
						{
							try {
								m.reset();
								t.call(bb, m);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} catch (InterruptedException e) {
					}
				}
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private List<HashingExecutor.HT> threads=new ArrayList<>();
	public void submitHashing(LazyFileHash t)
	{
		tasks.add(t);
	}
	synchronized public void setNThread(int n)
	{
		while(n>threads.size())
		{
			HT ht=new HT("Hash_"+threads.size());
			ht.setDaemon(true);
			threads.add(ht);
			ht.start();
		}
		while(n<threads.size())
		{
			HT ht=threads.remove(threads.size()-1);
			ht.exit=true;
		}
	}
}

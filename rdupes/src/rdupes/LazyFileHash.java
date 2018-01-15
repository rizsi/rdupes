package rdupes;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import hu.qgears.commons.UtilMd5;

public class LazyFileHash{
	private int originalChangeCounter;
	private File f;
	private volatile boolean cancelled;
	private long size;
	private RDupes rd;
	private String countedHash;
	private RDupesFile singleFile;
	private List<IHashListener> listeners=new ArrayList<>(0);
	public LazyFileHash(RDupesFile rDupesFile, long size) {
		this.singleFile=rDupesFile;
		rd=rDupesFile.rd;
		originalChangeCounter=rDupesFile.getChangeCounter();
		f=rDupesFile.file.toFile();
		this.size=size;
		rDupesFile.rd.hashing.submitHashing(this);
		rDupesFile.rd.nFileToHash.addAndGet(1);
		rDupesFile.rd.nBytesToHahs.addAndGet(size);
	}

	public void doWithHash(IHashListener sizeCluster) {
		synchronized (this) {
			if(countedHash!=null)
			{
				sizeCluster.hashCounted(singleFile, countedHash, originalChangeCounter);
			}else
			{
				if(listeners!=null)
				{
					listeners.add(sizeCluster);
				}
			}
		}
	}

	public String call(ByteBuffer buffer, MessageDigest m) throws Exception {
		long processed=0;
		String hash=null;
		try
		{
			if(cancelled)
			{
				throw new CancellationException("cancelled");
			}
			try(RandomAccessFile aFile = new RandomAccessFile(f, "r"))
			{
				FileChannel inChannel = aFile.getChannel();
				int n;
				buffer.clear();
				while((n=inChannel.read(buffer))>0)
				{
					buffer.flip();
					m.update(buffer);
					buffer.clear();
					if(cancelled)
					{
						throw new CancellationException("cancelled");
					}
					processed+=n;
					rd.nBytesToHahs.addAndGet(-n);
				}
				hash=UtilMd5.toMd5String(m);
			}
		}catch(CancellationException e)
		{
			hash=null;
		}catch(Exception e)
		{
			hash=null;
			// e.printStackTrace();
		}finally
		{
			List<IHashListener> hls;
			synchronized (this) {
				countedHash=hash;
				hls=listeners;
				listeners=null;
			}
			rd.nFileToHash.addAndGet(-1);
			rd.nBytesToHahs.addAndGet(-size+processed);
			for(IHashListener l: hls)
			{
				l.hashCounted(singleFile, hash, originalChangeCounter);
			}
		}
		return null;
	}

	public void cancel() {
		cancelled=true;
	}
}

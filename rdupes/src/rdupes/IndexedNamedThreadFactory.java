package rdupes;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory that sets the name of a
 * thread to something that is usable for the
 * developer.
 * @author rizsi
 *
 */
public class IndexedNamedThreadFactory implements ThreadFactory {

	protected String name;
	protected Integer priority;
	protected Boolean daemon;
	private AtomicInteger index=new AtomicInteger(1);
	
	public IndexedNamedThreadFactory(String name) {
		super();
		this.name = name;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread ret=new Thread(r, name+"_"+index.getAndIncrement());
		if(priority!=null)
		{
			ret.setPriority(priority);
		}
		if(daemon!=null)
		{
			ret.setDaemon(daemon);
		}
		return ret;
	}
	/**
	 * Set the priority of the thread to be created by this factory.
	 * @param priority null (default) means not to change default priority
	 * @return self
	 */
	public IndexedNamedThreadFactory setPriority(Integer priority) {
		this.priority = priority;
		return this;
	}

	/**
	 * Set whether the thread to be created is daemon.
	 * 
	 * @param daemon null(default) means not to change the default value.
	 * @return self
	 */
	public IndexedNamedThreadFactory setDaemon(Boolean daemon) {
		this.daemon = daemon;
		return this;
	}

}

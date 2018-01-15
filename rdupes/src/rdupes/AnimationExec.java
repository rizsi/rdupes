package rdupes;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

public class AnimationExec {
	static private AnimationExec instance=new AnimationExec();
	public static AnimationExec getInstance() {
		return instance;
	}
	private Object sycUiTaskCollector=new Object();
	private List<Runnable> uiTasks=new ArrayList<>();
	private List<Runnable> uiTasks2=new ArrayList<>();
	private Timeline updateStatus;
	public void runLater(Runnable runnable) {

		synchronized (sycUiTaskCollector) {
			uiTasks.add(runnable);

			if(updateStatus==null)
			{
				updateStatus = new Timeline(new KeyFrame(Duration.millis(100), new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						List<Runnable>ts=uiTasks;
						synchronized (sycUiTaskCollector) {
							uiTasks=uiTasks2;
							uiTasks2=ts;
						}
						if(ts.size()>0)
						{
//							System.out.println("Animation number of updates: "+ts.size());
						}
						for(Runnable r: ts)
						{
							r.run();
						}
						ts.clear();
					}
				}));
				updateStatus.setCycleCount(Timeline.INDEFINITE);
				updateStatus.play();
			}
		}
	}
}

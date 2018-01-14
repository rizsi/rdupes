package rdupes;

import hu.qgears.commons.UtilComma;
import hu.qgears.commons.UtilEventListener;
import javafx.application.Platform;
import javafx.scene.control.TreeCell;

public class RDTreeCell extends TreeCell<RDupesObject>{
	RDupesObject prevItem;
	private UtilEventListener<Boolean> l=new UtilEventListener<Boolean>() {
		@Override
		public void eventHappened(Boolean msg) {
			Platform.runLater(new Runnable() {
				
				@Override
				public void run() {
					updateText(prevItem);
				}
			});
		}
	};
	@Override
	protected void updateItem(RDupesObject item, boolean empty) {
		super.updateItem(item, empty);
		if(prevItem!=null)
		{
			prevItem.hasCollision.getPropertyChangedEvent().removeListener(l);
		}
		prevItem=item;
		updateText(item);
	}
	private void updateText(RDupesObject item) {
		if(item!=null)
		{
			StringBuilder targets=new StringBuilder();
			targets.append(item.toString());
			if(item.hasCollision.getProperty())
			{
				targets.append(" -> ");
				UtilComma c=new UtilComma(", ");
				for(RDupesObject coll: item.getCollisions())
				{
					targets.append(c.getSeparator());
					targets.append(coll.getFullName());
				}
			}
			setText(targets.toString());
		}else
		{
			setText(null);
		}
	}
}

package rdupes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hu.qgears.commons.UtilEventListener;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;

public class RDupesObjectTree extends TreeItem<RDupesObject>{
	private boolean opened=false;
	private UtilEventListener<RDupesObject> cl=new UtilEventListener<RDupesObject>() {
		@Override
		public void eventHappened(RDupesObject msg) {
			Platform.runLater(new Runnable() {
				
				@Override
				public void run() {
					updateChildren();
				}
			});
		}
	};
//	private UtilEventListener<Boolean> collisionListener=new UtilEventListener<Boolean>() {
//		public void eventHappened(Boolean msg) {
//			
//		};
//	};
	public RDupesObjectTree(RDupesObject value) {
		super(value);
		value.changed.addListener(cl);
//		value.hasCollision.getPropertyChangedEvent().addListener(collisionListener);
		expandedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				expanded(newValue);
			}
		});
	}
	/**
	 * TODO multiple updates fast after each other must be optimized
	 */
	private void updateChildren() {
		TreeItem<RDupesObject> parent=getParent();
		if(parent==null||parent.isExpanded())
		{
			List<RDupesObject> children=getValue().getChildren();
			Map<String, RDupesObjectTree> existingChildren=new HashMap<>();
			for(TreeItem<RDupesObject> ti: getChildren())
			{
				existingChildren.put(ti.getValue().getSimpleName(), (RDupesObjectTree)ti);
			}
			List<RDupesObjectTree> newChildren=new ArrayList<>();
			for(RDupesObject c:children)
			{
				RDupesObjectTree newChild=existingChildren.remove(c.getSimpleName());
				if(newChild!=null&&newChild.getValue()!=c)
				{
					newChild.dispose();
					newChild=null;
				}
				if(newChild==null)
				{
					newChild=new RDupesObjectTree(c);
				}
				newChildren.add(newChild);
			}
			for(RDupesObjectTree t: existingChildren.values())
			{
				t.dispose();
			}
			getChildren().clear();
			getChildren().addAll(newChildren);
			opened=true;
		}else
		{
		}
	}

	protected void expanded(boolean newValue) {
		for(TreeItem<RDupesObject> c: getChildren())
		{
			RDupesObjectTree st=(RDupesObjectTree) c;
			st.childrenFill(newValue);
		}
	}

	public void childrenFill(boolean newValue) {
		if(newValue)
		{
			if(!opened)
			{
				opened=true;
				RDupesObject o=getValue();
				for(RDupesObject c: o.getChildren())
				{
					RDupesObjectTree t=new RDupesObjectTree(c);
					getChildren().add(t);
				}
			}
		}else
		{
//			opened=false;
//			for(TreeItem<RDupesObject> c: getChildren())
//			{
//				RDupesObjectTree st=(RDupesObjectTree) c;
//				st.dispose();
//			}
//			getChildren().clear();
		}
	}

	private void dispose() {
		getValue().changed.removeListener(cl);
//		getValue().hasCollision.getPropertyChangedEvent().removeListener(collisionListener);
		for(TreeItem<RDupesObject> c: getChildren())
		{
			RDupesObjectTree st=(RDupesObjectTree) c;
			st.dispose();
		}
	}
}

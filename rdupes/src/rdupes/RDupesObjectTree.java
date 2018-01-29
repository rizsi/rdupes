package rdupes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import hu.qgears.commons.UtilEventListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;

public class RDupesObjectTree extends TreeItem<RDupesObject>{
	private boolean opened=false;
	private boolean shown=false;
	private AtomicInteger scheduled=new AtomicInteger(0);
	private Runnable updateOnUIThread=new Runnable() {
		
		@Override
		public void run() {
			scheduled.decrementAndGet();
			updateChildren();
		}
	};
	private UtilEventListener<RDupesObject> cl=new UtilEventListener<RDupesObject>() {
		@Override
		public void eventHappened(RDupesObject msg) {
			int sched=scheduled.get();
			if(sched==0)
			{
				scheduled.incrementAndGet();
				AnimationExec.getInstance().runLater(updateOnUIThread);
			}
		}
	};
	public RDupesObjectTree(RDupesObject value) {
		super(value);
		if(value==null)
		{
			return;
		}
		value.addChangeListener(cl);
		expandedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				expanded(newValue);
			}
		});
		valueProperty().addListener(new ChangeListener<RDupesObject>() {
			@Override
			public void changed(ObservableValue<? extends RDupesObject> observable, RDupesObject oldValue,
					RDupesObject newValue) {
				System.err.println("Object value changed!!!");
			}
		});
	}
	private void updateChildren() {
		RDupesObject rdo=getValue();
		if(rdo==null)
		{
			getChildren().clear();
			return;
		}
		if(!shown||!opened)
		{
			boolean has=getValue().hasChildren();
			if(getChildren().size()==0&&has)
			{
				getChildren().add(new RDupesObjectTree(null));
			}
			if(getChildren().size()>0&&!has)
			{
				getChildren().clear();
			}
		}else
		{
			TreeItem<RDupesObject> parent=getParent();
			if(parent==null||parent.isExpanded())
			{
				List<RDupesObject> children=rdo.getChildren();
				Map<String, RDupesObjectTree> existingChildren=new HashMap<>();
				List<RDupesObjectTree> toDelete=new ArrayList<>(0);
				for(TreeItem<RDupesObject> ti: getChildren())
				{
					RDupesObjectTree prev=existingChildren.put(ti.getValue().getSimpleName(), (RDupesObjectTree)ti);
					if(prev!=null)
					{
						toDelete.add(prev);
					}
				}
				List<RDupesObjectTree> newChildren=new ArrayList<>();
				boolean changed=false;
				for(RDupesObject c:children)
				{
					RDupesObjectTree newChild=existingChildren.remove(c.getSimpleName());
					if(newChild!=null&&newChild.getValue()!=c)
					{
						newChild.dispose();
						newChild=null;
						changed=true;
					}
					if(newChild==null)
					{
						changed=true;
						newChild=new RDupesObjectTree(c);
						if(isExpanded())
						{
							newChild.childrenFill(true);
						}
					}
					newChildren.add(newChild);
				}
				for(RDupesObjectTree t: existingChildren.values())
				{
					changed=true;
					t.dispose();
				}
				for(RDupesObjectTree t: toDelete)
				{
					t.dispose();
				}
				if(changed)
				{
					getChildren().clear();
					getChildren().addAll(newChildren);
				}
				opened=true;
			}else
			{
			}
		}
	}

	protected void expanded(boolean newValue) {
		if(!opened)
		{
			opened=true;
			shown=true;
			getChildren().clear();
			RDupesObject o=getValue();
			for(RDupesObject c: o.getChildren())
			{
				RDupesObjectTree t=new RDupesObjectTree(c);
				if(isExpanded())
				{
					t.childrenFill(true);
				}
				getChildren().add(t);
			}
		}
		for(TreeItem<RDupesObject> c: getChildren())
		{
			RDupesObjectTree st=(RDupesObjectTree) c;
			st.childrenFill(newValue);
		}
	}

	public void childrenFill(boolean newValue) {
		if(newValue)
		{
			if(!shown)
			{
				shown=true;
				if(!opened)
				{
					if(getValue().hasChildren())
					{
						getChildren().add(new RDupesObjectTree(null));
					}
				}
			}
		}else
		{
		}
	}

	private void dispose() {
		if(getValue()!=null)
		{
			getValue().removeChangeListener(cl);
		}
		for(TreeItem<RDupesObject> c: getChildren())
		{
			RDupesObjectTree st=(RDupesObjectTree) c;
			st.dispose();
		}
	}
}

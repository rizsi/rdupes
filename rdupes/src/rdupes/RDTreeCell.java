package rdupes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicInteger;

import hu.qgears.commons.UtilComma;
import hu.qgears.commons.UtilEventListener;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

public class RDTreeCell extends TreeCell<RDupesObject>{
	RDupesObject prevItem;
	
	public RDTreeCell() {
	}
	private UtilEventListener<RDupesObject> l=new UtilEventListener<RDupesObject>() {
		@Override
		public void eventHappened(RDupesObject msg) {
			scheduleUpdate();
		}
	};
	AtomicInteger scheduled=new AtomicInteger(0);
	private Runnable update=new Runnable() {
		
		@Override
		public void run() {
			scheduled.decrementAndGet();
			updateText(prevItem);
		}
	};
	private void scheduleUpdate()
	{
		if(scheduled.get()==0)
		{
			RDupesObject o=prevItem;
			if(o!=null)
			{
				scheduled.incrementAndGet();
				AnimationExec.getInstance().runLater(update);
			}
		}
	}
	@Override
	protected void updateItem(RDupesObject item, boolean empty) {
		super.updateItem(item, empty);
		if(prevItem!=null)
		{
			prevItem.removeChangeListener(l);
		}
		prevItem=item;
		if(prevItem!=null)
		{
			prevItem.addChangeListener(l);
		}
		updateText(item);
	}
	private void updateText(RDupesObject item) {
		if(item!=null)
		{
			StringBuilder targets=new StringBuilder();
			int childDupes=item.getChildDupes();
			long childDupesSize=item.getChildDupesSize();
			String s=item.toString();
			if(item.isAllCopy())
			{
				if(!getStyleClass().contains("copy"))
				{
					getStyleClass().add("copy");
				}
			}else
			{
				getStyleClass().remove("copy");
			}
			targets.append(s);
//			targets.append(" NF: "+item.nFile+" ND:"+item.getChildDupes());
//			targets.append(" FARTHEST: ");
//			targets.append(""+item.getDeepestLevel());
			targets.append(" ");
			targets.append(Integer.toString(item.nFile));
			targets.append(", ");
			targets.append(RDupesStage.formatMemory(item.getChildSize()));
			if(childDupes>0)
			{
				targets.append(" [");
				targets.append(Integer.toString(childDupes));
				targets.append(", ");
				targets.append(RDupesStage.formatMemory(childDupesSize));
				targets.append("] ");
			}
			if(item.hasCollision())
			{
				targets.append(" -> ");
				UtilComma c=new UtilComma(", ");
				for(RDupesObject coll: item.getCollisions())
				{
					targets.append(c.getSeparator());
					targets.append(coll.getFullName());
				}
			}
			if(item instanceof RDupesFolder)
			{
				RDupesFolder folder=(RDupesFolder) item;
				if(folder.getTrashDir()!=null)
				{
					targets.append(" TRASH TO: "+folder.getTrashDir());
				}
			}
			setText(targets.toString());
			if(item.isRootFolder())
			{
				final ContextMenu contextMenu = new ContextMenu();
				MenuItem mi=new MenuItem("Remove");
				mi.setOnAction(e->item.getHost().removeRootFolderFromModel((RDupesFolder)item));
				contextMenu.getItems().add(mi);
				mi=new MenuItem("Set trash folder...");
				mi.setOnAction(e->setupTrashFolder(item, e));
				contextMenu.getItems().add(mi);
				setContextMenu(contextMenu);
			}else if(item instanceof RDupes)
			{
				setContextMenu(null);
			}else if(item instanceof RDupesPath)
			{
				RDupesPath p=(RDupesPath)item;
				final ContextMenu contextMenu = new ContextMenu();
				MenuItem mi=new MenuItem("To Trash");
				mi.setOnAction(e->trashFile(p));
				contextMenu.getItems().add(mi);
				contextMenu.setOnShowing(e->mi.setDisable(p.getRootFolder().getTrashDir()==null));
				setContextMenu(contextMenu);
			}
		}else
		{
			getStyleClass().remove("copy");
			setText(null);
			setContextMenu(null);
		}
	}
	private Object trashFile(RDupesPath p) {
		File tg=p.getRootFolder().getTrashDir();
		Path rel=p.getRootFolder().file.relativize(p.file);
		Path tgPath=tg.toPath().resolve(rel);
		try {
			tgPath.toFile().getParentFile().mkdirs();
			if(p instanceof RDupesFolder && tgPath.toFile().exists())
			{
				Files.walkFileTree(p.file, new MergeMove(p.file, tgPath));
			}else
			{
				Files.move(p.file, tgPath, StandardCopyOption.ATOMIC_MOVE);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println("Move to: "+tgPath);
		return null;
	}
	private Object setupTrashFolder(RDupesObject item, ActionEvent e) {
		RDupesFolder f=(RDupesFolder)item;
		Window w=getTreeView().getScene().getWindow();
		DirectoryChooser trashDirChooser = new DirectoryChooser();
		trashDirChooser.setTitle("Select trash folder for root folder: "+f);
		File trashDir=trashDirChooser.showDialog(w);
		if(trashDir!=null)
		{
			f.setTrashDir(trashDir);
		}
		// TODO Host window becomes unresizable! https://bugs.openjdk.java.net/browse/JDK-8140491
		return null;
	}
}

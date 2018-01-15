package rdupes;

import java.io.File;
import java.text.DecimalFormat;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class RDupesStage {
	private RDupes rd;
	private Stage primaryStage;

	public RDupesStage(RDupes rd, Stage primaryStage) {
		this.rd = rd;
		this.primaryStage = primaryStage;
	}

	public void launch() {
		primaryStage.setTitle("RDupes duplicate file finder program.");

		RDupesObjectTree rootItem = new RDupesObjectTree(rd);
		rootItem.childrenFill(true);
		rootItem.setExpanded(true);
		rootItem.expanded(true);
		TreeView<RDupesObject> tree = new TreeView<RDupesObject>(rootItem);
		tree.setCellFactory(a -> new RDTreeCell());
		StackPane root = new StackPane();
		root.getChildren().add(tree);
		MenuBar menu = createMenu();
		Button heap=new Button();
		heap.setOnAction(e->System.gc());
		Label statusbar=new Label();
		VBox.setVgrow(root, Priority.ALWAYS);
		TextField nCores=new TextField();
		nCores.setPromptText("Number of hashing threads");
		nCores.textProperty().addListener(x->setNCores(nCores.getText()));
		nCores.setMinWidth(150);
		HBox tools=new HBox(heap, nCores);
		VBox box = new VBox(menu, root, statusbar, tools);
		primaryStage.setScene(new Scene(box,640, 480));
		Timeline updateStatus = new Timeline(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				statusbar.setText((rd.tasks.get()==0?"DONE ":"WORKING... ")+" Files indexed: "+rd.filesProcessed.get()+" in "+rd.foldersProcessed.get()+" folders ongoing hash: "+rd.nBytesToHahs.get()+" bytes in "+rd.nFileToHash.get()+" files");
				long free=Runtime.getRuntime().freeMemory();
				long total=Runtime.getRuntime().totalMemory();
				long max=Runtime.getRuntime().maxMemory();
				long alloc=total-free;
				int divisor=Math.max(rd.foldersProcessed.get(),1);
				heap.setText("Allocated: "+formatMemory(alloc)+" bytes of max: "+formatMemory(max)+" bytes/file: "+(alloc/divisor));
			}
		}));
		updateStatus.setCycleCount(Timeline.INDEFINITE);
		updateStatus.play();
	}

	private Object setNCores(String text) {
		try {
			rd.setNCores(Integer.parseInt(text));
		} catch (NumberFormatException e) {
		}
		return null;
	}

	DirectoryChooser addDirChooser = new DirectoryChooser();

	private MenuBar createMenu() {
		MenuBar menuBar = new MenuBar();

		// --- Menu File
		Menu menuFile = new Menu("File");

		MenuItem add = new MenuItem("Add root folder...");
		add.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				addDirChooser.setTitle("Add folder to find duplicates");
				File selected = addDirChooser.showDialog(primaryStage);
				if (selected != null) {
					if (validateSelectedFile(selected)) {
						rd.addFolder(selected);
					}
				}
			}
		});
		MenuItem newWindow = new MenuItem("Create new window");
		newWindow.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Stage secondStage = new Stage();
				new RDupesStage(rd, secondStage).launch();
				secondStage.show();
			}
		});
		menuFile.getItems().addAll(add, newWindow);

		menuBar.getMenus().addAll(menuFile);
		return menuBar;
	}

	protected boolean validateSelectedFile(File selected) {
		// TODO vlaidate that folders do not contain each other!
		return true;
	}
	private DecimalFormat df=new DecimalFormat("##.###");
	String formatMemory(long m)
	{
		long gig=1024l*1024l*1024l;
		long meg=1024l*1024l;
		long kilo=1024l;
		if(m>=gig)
		{
			double v=((double)m)/gig;
			return df.format(v)+"Gib";
		}
		if(m>=meg)
		{
			double v=((double)m)/meg;
			return df.format(v)+"Mib";
		}
		if(m>=kilo)
		{
			double v=((double)m)/kilo;
			return df.format(v)+"Kib";
		}
		return ""+m+" bytes";
	}

}

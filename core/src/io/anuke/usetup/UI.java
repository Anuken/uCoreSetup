package io.anuke.usetup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.setup.*;
import com.badlogic.gdx.setup.DependencyBank.ProjectDependency;
import com.badlogic.gdx.setup.DependencyBank.ProjectType;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Strings;

public class UI extends SceneModule{
	boolean home = System.getProperty("user.name").equals("anuke");
	ProjectBuilder builder;
	List<ProjectType> projects = new ArrayList<>();
	List<ProjectDependency> dependencies = new ArrayList<>();
	String projectName = "Test";
	String packageName = !home ? "some.package" : "io.anuke.test";
	String destination = !home ? System.getProperty("user.home") : "/home/anuke/Projects/Test";
	String[] templates = {"default", "gamejam", "simple"};
	String template = templates[0];
	
	Lwjgl3Graphics graphics = ((Lwjgl3Graphics)Gdx.graphics);
	
	Dialog buildDialog;
	Label buildLabel;
	int lastc = 0;
	
	@Override
	public void init(){
		Core.font.setUseIntegerPositions(true);
		
		projects.add(ProjectType.CORE);
		projects.add(ProjectType.DESKTOP);
		projects.add(ProjectType.HTML);
		
		dependencies.add(ProjectDependency.GDX);
		dependencies.add(ProjectDependency.CONTROLLERS);
		
		Gdx.graphics.setContinuousRendering(false);

		scene.table("button", t -> {

			t.row();

			t.table(prefs -> {
				float fw = 400;

				prefs.defaults().padTop(8);

				prefs.add("Name: ").left();
				prefs.addField(projectName, name -> projectName = name).width(fw);
				prefs.row();

				prefs.add("Package: ").left();
				prefs.addField(packageName, name -> packageName = name).width(fw);
				prefs.row();

				prefs.add("Destination: ");
				prefs.addField(destination, name -> destination = name).width(fw);
			});

			t.row();

			t.table("button", temp -> {
				temp.marginTop(12).margin(14f).left();
				temp.add("Template:").left().padBottom(6).left();

				temp.row();
				temp.table(groups -> {
					ButtonGroup<CheckBox> group = new ButtonGroup<>();

					for(String type : templates){
						groups.addCheck(Strings.capitalize(type), type.equals(template), b -> template = type)
							.group(group).pad(4).left().padRight(8).padLeft(0).fill();
					}
				});
			});

			t.row();

			t.table("button", proj -> {
				proj.marginTop(12).margin(14f).left();

				proj.add("Sub-projects:").left().padBottom(6).left();
				proj.row();

				proj.table(c -> {
					for(ProjectType type : ProjectType.values()){
						c.addCheck(Strings.capitalize(type.getName()),
						projects.contains(type), b->{
							if(b){
								projects.add(type);
							}else{
								projects.remove(type);
							}
						}).pad(4).left().padRight(8).padLeft(0);
					}
				});

			}).fillX();

			t.row();

			t.table("button", ext -> {
				ext.margin(14);

				ext.add("Extensions:").left().padBottom(6);

				ext.row();

				ProjectDependency[] depend = ProjectDependency.values();
				int amount = ProjectDependency.values().length;
				int max = 5;

				Table current = new Table();

				ext.add(current);

				for(int i = 0 ; i < amount; i ++){
					if(i % max == 0){
						current.row();
					}

					int idx = i;

					current.addCheck(Strings.capitalize(depend[i].name().toLowerCase()),
					dependencies.contains(depend[i]), b -> {
						if(b){
							if(!dependencies.contains(depend[idx])) dependencies.add(depend[idx]);
						}else{
							dependencies.remove(depend[idx]);
						}
					}).left().pad(4).padLeft(0);
				}

			}).fillX();

			t.row();

			t.addButton("Generate", this::generate).padTop(10).fill().height(60);
		});

		scene.table(t -> t.top().add("uCore Project Setup").padTop(12).color(Color.CORAL).get().setFontScale(1f));
		
		scene.table(t -> {
			float sz = 50;

			t.top().right();
			t.marginTop(0).marginRight(0);
			
			t.addButton("-", graphics.getWindow()::iconifyWindow).size(sz);
			t.addButton("X", Gdx.app::exit).size(sz);
		});
	}
	
	void generate(){
		DependencyBank bank = new DependencyBank();
		builder = new ProjectBuilder(bank);
		
		List<Dependency> list = new ArrayList<>();
		
		for(ProjectDependency dep : dependencies) {
			list.add(bank.getDependency(dep));
		}
		
		List<String> incompat = builder.buildProject(projects, list);
		
		if(!incompat.isEmpty()){
			Dialog dialog = new Dialog("Incompatible Extensions");
			dialog.content().add("The following errors occured: ");
			dialog.content().row();
			for(String s : incompat){
				dialog.content().add("- " + s).left();
				dialog.content().row();
			}
			dialog.content().add("Do you wish to continue?");
			
			dialog.getButtonTable().addButton("OK", ()->{
				dialog.hide();
				build();
			});
			
			dialog.getButtonTable().addButton("Cancel", dialog::hide);
			
			dialog.show();
		}else{
			build();
		}
	}
	
	void build(){
		try{
			builder.build();
		}catch(IOException e){
			e.printStackTrace();
			new TextDialog("Error.", e.getClass().getSimpleName() + ": " + e.getMessage()).show();
		}
		
		callSetup();
	}
	
	void callSetup(){
		lastc = 0;
		buildDialog = new Dialog("Project Log", "dialog");
		buildLabel = new Label("");

		Table inner = new Table().margin(20);
		inner.add(buildLabel);

		ScrollPane pane = new ScrollPane(inner);
		pane.setFadeScrollBars(false);
		
		buildDialog.content().add(pane).grow().padTop(8);
		
		new Thread(() -> {
			printlog("Generating app in " + destination + "...");

			try {
				new GdxSetup().build(builder, template, destination, projectName, packageName,
						projectName, "/home/anuke/Android/Sdk", this::printlog, new ArrayList<>());
			}catch (Exception e){
				Gdx.app.postRunnable(() -> {
					Dialog d = new Dialog("Error generating project!", "dialog");
					d.content().add(Strings.parseException(e, true));
					d.addCloseButton();
					d.show();
				});
				return;
			}

			printlog("Done!");
			
			Gdx.app.postRunnable(()->{
				buildLabel.invalidateHierarchy();
				buildDialog.invalidateHierarchy();
				buildDialog.content().invalidateHierarchy();
				buildDialog.pack();
				
				buildDialog.buttons().addButton("OK", buildDialog::hide).width(100f);
				buildDialog.buttons().addButton("Exit", Gdx.app::exit).width(100f);
			});
		}).start();
		
		buildDialog.show();
	}
	
	void printlog(String str){
		System.out.println(str);
		
		buildLabel.getText().append(str + "\n");
	}
	
	void printlog(char c){
		System.out.print(c);

		Gdx.app.postRunnable(() -> {
			if(c == '\n') lastc = 0;

			buildLabel.getText().append(c);

			if(lastc > 62){
				buildLabel.getText().append("\n");
				lastc = 0;
			}else{
				lastc ++;
			}
		});
	}
	
	@Override
	public void update(){
		Graphics.clear(Color.BLACK);
		super.update();
		Timers.update();
	}
	
}

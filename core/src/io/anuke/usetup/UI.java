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
import com.badlogic.gdx.setup.Executor.CharCallback;

import io.anuke.ucore.core.DrawContext;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Strings;
import io.anuke.ucore.util.Timers;

//TODO gamejam ecs template?
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
		DrawContext.font.setUseIntegerPositions(true);
		
		projects.add(ProjectType.CORE);
		projects.add(ProjectType.DESKTOP);
		projects.add(ProjectType.HTML);
		
		dependencies.add(ProjectDependency.GDX);
		
		Gdx.graphics.setContinuousRendering(false);
		
		build.begin();
		
		new table("button"){{
			
			row();
			
			new table(){{
			
				defaults().padTop(8);
				
				new label("Name: ").left();
				
				float fw = 400;
				
				new field(projectName, name->{
					projectName = name;
				}).width(fw);
				
				row();
				
				new label("Package: ").left();
				
				new field(packageName, name->{
					packageName = name;
				}).width(fw);
				
				row();
				
				new label("Destination: ");
				
				new field(destination, name->{
					destination = name;
				}).width(fw);
			
			}}.end();
			
			row();
			
			new table("button"){{
				padTop(12);
				aleft();
				get().pad(14);
				
				new label("Template:").left().padBottom(6).left();
				
				row();
				
				new table(){{
					ButtonGroup<CheckBox> group = new ButtonGroup<>();
					
					for(String type : templates){
						new checkbox(Strings.capitalize(type), 
								type.equals(template), b->{
									
							template = type;
						}){{
							group.add(get());
						}}.pad(4).left().padRight(8).padLeft(0).fill();
					}
				}}.end();
				
			}}.fillX().end();
			
			row();
			
			new table("button"){{
				padTop(12);
				aleft();
				get().pad(14);
				
				new label("Sub-projects:").left().padBottom(6).left();
				
				row();
				
				new table(){{
					for(ProjectType type : ProjectType.values()){
						new checkbox(Strings.capitalize(type.getName()), 
								projects.contains(type), b->{
									
							if(b){
								projects.add(type);
							}else{
								projects.remove(type);
							}
						}).pad(4).left().padRight(8).padLeft(0);
					}
				}}.end();
				
			}}.fillX().end();
			
			row();
			
			new table("button"){{
				padTop(12);
				get().pad(14);
				
				new label("Extensions:").left().padBottom(6);
				
				row();
				
				ProjectDependency[] depend = ProjectDependency.values();
				int amount = ProjectDependency.values().length;
				int max = 5;
				
				Table current = new Table();
				
				add(current);
				
				for(int i = 0 ; i < amount; i ++){
					if(i % max == 0){
						current.row();
					}
					
					int idx = i;
					
					current.addCheck(Strings.capitalize(depend[i].name().toLowerCase()), 
							dependencies.contains(depend[i]), b->{
						
						if(b){
							dependencies.add(depend[idx]);
						}else{
							dependencies.remove(depend[idx]);
						}
					}).left().pad(4).padLeft(0);
				}
				
			}}.fillX().end();
			
			row();
			
			new button("Generate", ()->{
				generate();
			}).padTop(10).fill().height(60);
			
		}}.end();
		
		new table(){{
			atop().aright();
			
			get().padTop(0).padRight(0);
			
			float sz = 50;
			
			new button("-", ()->{
				graphics.getWindow().iconifyWindow();
			}).size(sz);
			
			new button("X", ()->{
				Gdx.app.exit();
			}).size(sz);
			
		}}.end();
		
		new table(){{
			atop();
			new label("uCore Project Setup").scale(1f).padTop(12).color(Color.CORAL);
		}}.end();
		
		build.end();
	}
	
	void generate(){
		DependencyBank bank = new DependencyBank();
		builder = new ProjectBuilder(bank);
		
		List<Dependency> list = new ArrayList<>();
		
		for(ProjectDependency dep : dependencies)
			list.add(bank.getDependency(dep));
		
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
			
			dialog.getButtonTable().addButton("Cancel", ()->{
				dialog.hide();
			});
			
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
		
		buildDialog.content().add(buildLabel).padTop(8);
		
		new Thread(()->{
			printlog("Generating app in " + destination + "...");

			new GdxSetup().build(builder, template, destination, projectName, packageName, projectName, "/home/anuke/Android/Sdk", new CharCallback() {
				@Override
				public void character (char c) {
					printlog(c);
				}
			}, new ArrayList<String>());
			printlog("Done!");
			
			Gdx.app.postRunnable(()->{
				buildLabel.pack();
				buildDialog.invalidateHierarchy();
				buildDialog.content().invalidateHierarchy();
				buildDialog.pack();
				
				buildDialog.button("OK", true);
				buildDialog.getButtonTable().addButton("Exit", ()->{
					Gdx.app.exit();
				});
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
		
		if(c == '\n') lastc = 0;
		
		buildLabel.getText().append(c);
		
		if(lastc > 62){
			buildLabel.getText().append("\n");
			lastc = 0;
		}else{
			lastc ++;
		}
	}
	
	@Override
	public void update(){
		clearScreen(Color.BLACK);
		super.update();
		Timers.update();
	}
	
}

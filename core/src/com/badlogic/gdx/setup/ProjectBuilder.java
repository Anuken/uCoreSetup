/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.setup;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.setup.DependencyBank.ProjectType;

public class ProjectBuilder {

	DependencyBank bank;
	List<ProjectType> modules = new ArrayList<>();
	List<Dependency> dependencies = new ArrayList<>();
	File settingsFile;
	File buildFile;

	public ProjectBuilder(DependencyBank bank) {
		this.bank = bank;
	}

	public List<String> buildProject(List<ProjectType> projects, List<Dependency> dependencies) {
		List<String> incompatibilities = new ArrayList<>();
		for (Dependency dep : dependencies) {
			for (ProjectType type : projects) {
				dep.getDependencies(type);
				incompatibilities.addAll(dep.getIncompatibilities(type));
			}
		}
		this.modules = projects;
		this.dependencies = dependencies;
		return incompatibilities;
	}

	public boolean build() throws IOException {
		settingsFile = File.createTempFile("libgdx-setup-settings", ".gradle");
		buildFile = File.createTempFile("libgdx-setup-build", ".gradle");
		if (!settingsFile.exists()) {
			settingsFile.createNewFile();
		}
		if (!buildFile.exists()) {
			buildFile.createNewFile();
		}
		settingsFile.setWritable(true);
		buildFile.setWritable(true);
		try {
			FileWriter settingsWriter = new FileWriter(settingsFile.getAbsoluteFile());
			BufferedWriter settingsBw = new BufferedWriter(settingsWriter);
			String settingsContents = "include ";
			int i = 0;
			for (ProjectType module : modules) {
				settingsContents += "'" + module.getName() + "'";
				if (i ++ != modules.size() - 1) {
					settingsContents += ", ";
				}
			}
			settingsContents += '\n';

			settingsContents += new String(Files.readAllBytes(Paths.get("templates/base/settings.gradle")));

			settingsBw.write(settingsContents);
			settingsBw.close();
			settingsWriter.close();

			FileWriter buildWriter = new FileWriter(buildFile.getAbsoluteFile());
			BufferedWriter buildBw = new BufferedWriter(buildWriter);

			BuildScriptHelper.addBuildScript(modules, buildBw);
			BuildScriptHelper.addAllProjects(buildBw);

			for (ProjectType module : modules) {
				BuildScriptHelper.addProject(module, dependencies, buildBw);
			}

			//Add task here for now
			buildBw.write("\n");
			buildBw.write("tasks.eclipse.doLast {\n");
			buildBw.write("    delete \".project\"\n");
			buildBw.write("}");

			buildBw.close();
			buildWriter.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void cleanUp() {
		settingsFile.deleteOnExit();
		buildFile.deleteOnExit();
	}

}

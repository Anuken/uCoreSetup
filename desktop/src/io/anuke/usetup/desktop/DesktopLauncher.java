package io.anuke.usetup.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import io.anuke.usetup.UCoreSetup;

public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("uCore Project Setup");
		config.setWindowedMode(800, 700);
		config.setDecorated(false);
		config.setResizable(false);
		new Lwjgl3Application(new UCoreSetup(), config);
	}
}

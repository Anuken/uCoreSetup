package io.anuke.usetup;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.anuke.ucore.core.Core;
import io.anuke.ucore.modules.ModuleCore;

public class UCoreSetup extends ModuleCore {
	
	@Override
	public void init(){
		Core.batch = new SpriteBatch();
		module(new UI());
	}
	
}

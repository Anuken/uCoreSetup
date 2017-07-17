package io.anuke.usetup;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.anuke.ucore.core.DrawContext;
import io.anuke.ucore.modules.Core;

public class UCoreSetup extends Core {
	
	@Override
	public void init(){
		DrawContext.batch = new SpriteBatch();
		add(new UI());
	}
	
}

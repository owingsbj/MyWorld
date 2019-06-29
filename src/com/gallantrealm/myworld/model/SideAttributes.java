package com.gallantrealm.myworld.model;

import java.io.IOException;
import java.io.Serializable;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.Sendable;

public final class SideAttributes implements Serializable, Cloneable, Sendable {
	static final long serialVersionUID = 1L;

	private static SideAttributes defaultSideAttributes;

	public static SideAttributes getDefaultSideAttributes() {
		if (defaultSideAttributes == null) {
			defaultSideAttributes = new SideAttributes();
			defaultSideAttributes.isDefault = true;
		}
		return defaultSideAttributes;
	}

	public float red = 1.0f;
	public float green = 1.0f;
	public float blue = 1.0f;
	public float transparency;
	public float shininess = 0.0f;
	public boolean fullBright;
	public String textureURL;
	public float textureScaleX = 1.0f;
	public float textureScaleY = 1.0f;
	public float textureRotation;
	public float textureOffsetX = 0.5f;
	public float textureOffsetY = 0.5f;
	public float textureVelocityX;
	public float textureVelocityY;
	public float textureAMomentum;
	public long textureRefreshInterval;
	public boolean bumpMap;  // keep, for compatibility..
	public boolean alphaTest;
	public boolean isDefault;
	public boolean pixelate;

	public SideAttributes() {
	}

	@Override
	protected Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void send(DataOutputStreamX os) throws IOException {
		os.writeFloat(red);
		os.writeFloat(green);
		os.writeFloat(blue);
		os.writeFloat(transparency);
		os.writeFloat(shininess);
		os.writeBoolean(fullBright);
		os.writeString(textureURL);
		os.writeFloat(textureScaleX);
		os.writeFloat(textureScaleY);
		os.writeFloat(textureRotation);
		os.writeFloat(textureOffsetX);
		os.writeFloat(textureOffsetY);
		os.writeFloat(textureVelocityX);
		os.writeFloat(textureVelocityY);
		os.writeFloat(textureAMomentum);
		os.writeLong(textureRefreshInterval);
		os.writeBoolean(alphaTest);
		os.writeBoolean(pixelate);
	}

	@Override
	public void receive(DataInputStreamX is) throws IOException {
		red = is.readFloat();
		green = is.readFloat();
		blue = is.readFloat();
		transparency = is.readFloat();
		shininess = is.readFloat();
		fullBright = is.readBoolean();
		textureURL = is.readString();
		textureScaleX = is.readFloat();
		textureScaleY = is.readFloat();
		textureRotation = is.readFloat();
		textureOffsetX = is.readFloat();
		textureOffsetY = is.readFloat();
		textureVelocityX = is.readFloat();
		textureVelocityY = is.readFloat();
		textureAMomentum = is.readFloat();
		textureRefreshInterval = is.readLong();
		alphaTest = is.readBoolean();
		pixelate = is.readBoolean();
	}

}

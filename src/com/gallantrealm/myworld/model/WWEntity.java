package com.gallantrealm.myworld.model;

import java.io.IOException;
import java.io.Serializable;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.Sendable;

/**
 * An entity is an abstract class. Every object and user within the world, and even the world itself, is an entity. This class provides some common attributes and methods for both objects and users.
 */
public abstract class WWEntity extends WWConstant implements Serializable, Cloneable, Sendable {

	static final long serialVersionUID = 1L;

	public WWWorld world;

	private String name;
	private String description;
	private long createTime;
	private long lastModifyTime;
	public boolean deleted;

	private final WWWorld getWorld() {
		return world;
	}

	public void setWorld(WWWorld world) {
		this.world = world;
	}

	/**
	 * Returns the current world time if in-world, or the current system time if not.
	 */
	public long getWorldTime() {
		if (world != null) {
			return world.worldTime; // getWorldTime();
		} else {
			return 0;
		}
	}

	@Override
	public Object clone()  {
		WWEntity clone;
		try {
			clone = (WWEntity) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
		clone.setWorld(null); // since clone doesn't belong to a world, yet
		return clone;
	}

	/**
	 * Send the properties of this entity to a stream. This is used to stream the state of the entity from the server to clients. (Note that Object serialization is not used so that the streamed data is in a reasonably compact form.)
	 * Subclasses should override this method, but first send their properties before calling the superclass, as the superclass will set timestamps used for update detection.
	 **/
	public void send(DataOutputStreamX os) throws IOException {
		os.writeString(name);
		os.writeString(description);
		os.writeLong(createTime);
		os.writeLong(lastModifyTime);
	}

	/**
	 * Receive the streamed properties of the entity. Subclasses should override this method, receiving the properties specific to this class, then calling the superclass.
	 **/
	public void receive(DataInputStreamX is) throws IOException {
		// Note: assumed id already received (in order to determine object to update
		name = is.readString();
		description = is.readString();
		createTime = is.readLong();
		lastModifyTime = is.readLong();
	}

	public final long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public final boolean isDeleted() {
		return deleted;
	}

	public void setDeleted() {
		deleted = true;
	}

	public final long getLastModifyTime() {
		return lastModifyTime;
	}

	public void setLastModifyTime(long lastModifyTime) {
		this.lastModifyTime = lastModifyTime;
	}

	public final String getName() {
		if (name == null) {
			return this.getClass().getSimpleName();
		}
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public final String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		String id = "";
		if (name != null) {
			id += name;
		}
		id += " - ";
		if (description != null) {
			id += description;
		}
		id += " - ";
		id += getClass().getName();
		if (world != null) {
			if (this instanceof WWObject) {
				int objectId = world.getObjectId((WWObject) this);
				id += " ";
				id += objectId;
			}
		}
		return id;
	}

}

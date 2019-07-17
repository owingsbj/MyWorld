package com.gallantrealm.myworld.model.behavior;

import java.util.ArrayList;
import com.gallantrealm.myworld.model.WWEntity;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWWorld;
import com.gallantrealm.myworld.model.WWWorld.ActionParams;

public class BehaviorThread extends Thread {

	private class BehaviorRequest {
		public String event; // the act
		public WWObject object; // the object being acted upon
		public WWEntity agent; // the user or object doing the action
		public Object params; // more details on the act
	}

	private final WWWorld world;
	private final ArrayList<BehaviorRequest> requestQueue;
	private final Object queueSemaphore;
	public boolean safeStop;

	public BehaviorThread(WWWorld world) {
		setName("BehaviorThread");
		this.world = world;
		requestQueue = new ArrayList<BehaviorRequest>();
		queueSemaphore = new Object();
		setDaemon(true);
	}

	public void queue(String event, WWObject object, WWEntity user, Object params) {
		// If an request already exists for this on the queue, then update the request with the new params,
		// otherwise queue a new request
		synchronized (queueSemaphore) {
			for (int i = 0; i < requestQueue.size(); i++) {
				BehaviorRequest queuedRequest = requestQueue.get(i);
				if (queuedRequest.event.equals(event) && queuedRequest.object == object && queuedRequest.agent == user) {
					queuedRequest.params = params;
					return;
				}
			}
			BehaviorRequest request = new BehaviorRequest();
			request.event = event;
			request.object = object;
			request.agent = user;
			request.params = params;
			requestQueue.add(request);
			queueSemaphore.notify();
		}
	}

	@Override
	public void run() {
		while (!safeStop) {
			try {
				BehaviorRequest request;
				synchronized (queueSemaphore) {
					if (requestQueue.isEmpty()) {
						queueSemaphore.wait();
					}
					request = requestQueue.remove(0);
				}
				// TODO add timer to force interruption if event runs too long
				if (request.object != null) {
					request.object.invokeBehavior(request.event, request.agent, request.params);
				} else {
					world.invokeAction(request.event, request.agent, (ActionParams) request.params);
				}
				//Thread.sleep(25); // throttle speed
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				e.printStackTrace();
				// TODO send a message to the user if known to let them know the behavior failed
			}
		}
	}

}

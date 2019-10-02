package com.untamedears.JukeAlert.model.actions;

import java.util.UUID;

public abstract class LoggablePlayerAction extends PlayerAction implements LoggableAction {
	
	private ActionCacheState state;
	private int id;

	public LoggablePlayerAction(long time, UUID player) {
		super(time, player);
		state = ActionCacheState.NEW;
	}
	
	
	@Override
	public LoggedActionPersistence getPersistence() {
		return new LoggedActionPersistence(player, null, time, null);
	}
	
	public void setID(int id) {
		this.id = id;
		state = ActionCacheState.NORMAL;
	}
	
	public int getID() {
		return id;
	}
	
	public void setCacheState(ActionCacheState state) {
		this.state = state;
	}
	
	public ActionCacheState getCacheState() {
		return state;
	}

	@Override
	public boolean isLifeCycleEvent() {
		return false;
	}

}

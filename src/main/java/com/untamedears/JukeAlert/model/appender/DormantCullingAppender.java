package com.untamedears.JukeAlert.model.appender;

import org.bukkit.configuration.ConfigurationSection;

import com.untamedears.JukeAlert.JukeAlert;
import com.untamedears.JukeAlert.model.Snitch;
import com.untamedears.JukeAlert.model.actions.LoggablePlayerAction;
import com.untamedears.JukeAlert.model.actions.SnitchAction;
import com.untamedears.JukeAlert.model.actions.internal.DestroySnitchAction;
import com.untamedears.JukeAlert.model.actions.internal.DestroySnitchAction.Cause;
import com.untamedears.JukeAlert.model.appender.config.DormantCullingConfig;
import com.untamedears.JukeAlert.util.JukeAlertPermissionHandler;

import vg.civcraft.mc.civmodcore.util.BukkitComparators;
import vg.civcraft.mc.civmodcore.util.progress.ProgressTrackable;

public class DormantCullingAppender extends ConfigurableSnitchAppender<DormantCullingConfig>
		implements ProgressTrackable {
	
	public static final String ID = "dormantcull";

	private long lastRefresh;
	private long nextUpdate;

	public DormantCullingAppender(Snitch snitch, ConfigurationSection config) {
		super(snitch, config);
		if (snitch.getId() == -1) {
			// snitch was just created
			lastRefresh = System.currentTimeMillis();
		} else {
			lastRefresh = JukeAlert.getInstance().getDAO().getRefreshTimer(snitch.getId());
			if (lastRefresh == -1) {
				// no data in db due to recent config change, let's use the current time and
				// mark it for saving later
				refreshTimer();
			}
		}
		nextUpdate = calcFutureUpdate();
		JukeAlert.getInstance().getSnitchCullManager().addCulling(this);
		updateState();
	}

	@Override
	public boolean runWhenSnitchInactive() {
		return true;
	}

	@Override
	public void acceptAction(SnitchAction action) {
		if (action.isLifeCycleEvent()) {
			if (action instanceof DestroySnitchAction) {
				JukeAlert.getInstance().getSnitchCullManager().removeCulling(this);
			}
			return;
		}
		if (!action.hasPlayer()) {
			return;
		}
		LoggablePlayerAction playerAction = (LoggablePlayerAction) action;
		if (snitch.hasPermission(playerAction.getPlayer(), JukeAlertPermissionHandler.getListSnitches())) {
			refreshTimer();
		}
	}

	public long getLastRefresh() {
		return lastRefresh;
	}

	public void refreshTimer() {
		this.lastRefresh = System.currentTimeMillis();
		snitch.setDirty();
		updateState();
		JukeAlert.getInstance().getSnitchCullManager().updateCulling(this, calcFutureUpdate());
	}

	public long getTimeSinceLastRefresh() {
		return System.currentTimeMillis() - lastRefresh;
	}

	@Override
	public Class<DormantCullingConfig> getConfigClass() {
		return DormantCullingConfig.class;
	}

	@Override
	public void persist() {
		if (snitch.getId() != -1) {
			JukeAlert.getInstance().getDAO().setRefreshTimer(snitch.getId(), lastRefresh);
		}
	}

	@Override
	public int compareTo(ProgressTrackable o) {
		return BukkitComparators.getLocation().compare(((AbstractSnitchAppender) o).getSnitch().getLocation(),
				snitch.getLocation());
	}

	@Override
	public void updateInternalProgressTime(long update) {
		this.nextUpdate = update;
	}

	private long calcFutureUpdate() {
		long elapsed = getTimeSinceLastRefresh();
		if (elapsed >= config.getTotalLifeTime()) {
			return Long.MAX_VALUE;

		}
		if (elapsed >= config.getLifetime()) {
			return lastRefresh + config.getTotalLifeTime();
		} else {
			return lastRefresh + config.getLifetime();
		}
	}

	@Override
	public void updateState() {
		long elapsed = getTimeSinceLastRefresh();
		if (elapsed >= config.getTotalLifeTime()) {
			JukeAlert.getInstance().getSnitchManager().removeSnitch(snitch);
			snitch.processAction(new DestroySnitchAction(System.currentTimeMillis(), snitch, null, Cause.CULL));
			return;
		}
		if (elapsed >= config.getLifetime()) {
			snitch.setActiveStatus(false);
		}
	}

	@Override
	public long getNextUpdate() {
		return nextUpdate;
	}

}
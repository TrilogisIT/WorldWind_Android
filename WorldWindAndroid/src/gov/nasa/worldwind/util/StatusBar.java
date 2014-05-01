/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;
import gov.nasa.worldwind.R;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author tag
 * @version $Id$
 */
public class StatusBar extends RelativeLayout implements PositionListener, RenderingListener
{
	// Units constants TODO: Replace with UnitsFormat
	public final static String UNIT_METRIC = "gov.nasa.worldwind.StatusBar.Metric";
	public final static String UNIT_IMPERIAL = "gov.nasa.worldwind.StatusBar.Imperial";

	protected static final int MAX_ALPHA = 254;

	private WorldWindow eventSource;
	private String elevationUnit = UNIT_METRIC;
	private String angleFormat = Angle.ANGLE_FORMAT_DD;

	protected TextView onlineDisplay;
	protected TextView latDisplay;
	protected TextView lonDisplay;
	protected TextView altDisplay;
	protected TextView eleDisplay;
	protected TextView heartBeat;
	protected Drawable offlineDrawable = getResources().getDrawable(R.drawable.ic_action_network_wifi_off_status);
	protected Drawable onlineDrawable = getResources().getDrawable(R.drawable.ic_action_network_wifi_status);
	protected Drawable downloadDrawable = getResources().getDrawable(R.drawable.ic_action_download_status);

	protected AtomicBoolean showNetworkStatus = new AtomicBoolean(true);
	protected AtomicBoolean isNetworkAvailable = new AtomicBoolean(true);
	protected Thread netCheckThread;
	protected ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

	private static final int WHAT_NETWORK_STATUS = 0;
	private static final int WHAT_ELEVATION_CHANGE = 1;
	private static final int WHAT_POSITION_CHANGE = 2;


	private Handler uiHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case WHAT_NETWORK_STATUS:
					heartBeat.setVisibility(isShowNetworkStatus() ? VISIBLE : INVISIBLE);
					if (!isShowNetworkStatus())
						return;

					if (!isNetworkAvailable.get()) {
						heartBeat.setText(Logging.getMessage("term.NoNetwork"));
						heartBeat.setTextColor(Color.argb(MAX_ALPHA, 255, 0, 0));
						heartBeat.setCompoundDrawables(offlineDrawable, null, null, null);
						return;
					}

					int alpha = Color.alpha(heartBeat.getCurrentTextColor());
					if (isNetworkAvailable.get() && WorldWind.getRetrievalService().hasActiveTasks()) {
						heartBeat.setText(Logging.getMessage("term.Downloading"));
						heartBeat.setCompoundDrawables(onlineDrawable, null, downloadDrawable, null);
						if (alpha >= MAX_ALPHA)
							alpha = MAX_ALPHA;
						else
							alpha = alpha < 16 ? 16 : Math.min(MAX_ALPHA, alpha + 20);
					} else {
						alpha = Math.max(0, alpha - 20);
						heartBeat.setCompoundDrawables(onlineDrawable, null, null, null);
					}
					heartBeat.setTextColor(Color.argb(alpha, 255, 0, 0));
					break;
				case WHAT_POSITION_CHANGE:
//					Position newPos = msg.getData().getParcelable("position");
					Position newPos = (Position) msg.obj;
					if (newPos != null)
					{
						String las = makeAngleDescription("Lat", newPos.getLatitude());
						String los = makeAngleDescription("Lon", newPos.getLongitude());
						String els = makeCursorElevationDescription(
								eventSource.getModel().getGlobe().getElevation(newPos.getLatitude(), newPos.getLongitude()));
						latDisplay.setText(las);
						lonDisplay.setText(los);
						eleDisplay.setText(els);
					}
					else
					{
						latDisplay.setText("");
						lonDisplay.setText(Logging.getMessage("term.OffGlobe"));
						eleDisplay.setText("");
					}
					break;
				case WHAT_ELEVATION_CHANGE:
					if (eventSource.getView() != null && eventSource.getView().getEyePosition() != null)
						altDisplay.setText(makeEyeAltitudeDescription(
								eventSource.getView().getEyePosition().elevation));
					else
						altDisplay.setText(Logging.getMessage("term.Altitude"));
					break;
			}
		}
	};

	public StatusBar(Context context, AttributeSet attr) {
		this(context, attr, 0);
	}

	public StatusBar(Context context, AttributeSet attr, int defStyle)
	{
		super(context, attr, defStyle);

		LayoutInflater.from(context).inflate(R.layout.statusbar, this, true);

		onlineDisplay = (TextView) findViewById(R.id.online);
		latDisplay = (TextView) findViewById(R.id.latitude);
		lonDisplay = (TextView) findViewById(R.id.longitude);
		lonDisplay.setText(Logging.getMessage("term.OffGlobe"));
		altDisplay = (TextView) findViewById(R.id.altitude);
		eleDisplay = (TextView) findViewById(R.id.elevation);
		heartBeat = (TextView) findViewById(R.id.heartBeat);
		heartBeat.setText(Logging.getMessage("term.Downloading"));
		heartBeat.setTextColor(Color.RED);

		WorldWind.getNetworkStatus().addPropertyChangeListener(NetworkStatus.HOST_UNAVAILABLE,
				new PropertyChangeListener()
				{
					public void propertyChange(PropertyChangeEvent evt)
					{
						Object nv = evt.getNewValue();
						String message = Logging.getMessage("NetworkStatus.UnavailableHost",
								nv != null && nv instanceof URL ? ((URL) nv).getHost() : "Unknown");
						Logging.info(message);
					}
				});

		WorldWind.getNetworkStatus().addPropertyChangeListener(NetworkStatus.HOST_AVAILABLE,
				new PropertyChangeListener()
				{
					public void propertyChange(PropertyChangeEvent evt)
					{
						Object nv = evt.getNewValue();
						String message = Logging.getMessage("NetworkStatus.HostNowAvailable",
								nv != null && nv instanceof URL ? ((URL) nv).getHost() : "Unknown");
						Logging.info(message);
					}
				});
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if(isShowNetworkStatus())
			netCheckThread = startNetCheckThread();
		executorService.scheduleAtFixedRate
				(new Runnable() {
					public void run() {
						uiHandler.sendEmptyMessage(WHAT_NETWORK_STATUS);
					}
				}, 0, 100, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void onDetachedFromWindow() {
		executorService.shutdown();
		if(netCheckThread!=null)
			netCheckThread.interrupt();
		super.onDetachedFromWindow();
	}

	protected NetworkCheckThread startNetCheckThread()
	{
		NetworkCheckThread nct = new NetworkCheckThread(this.showNetworkStatus, this.isNetworkAvailable, null);
		nct.setDaemon(true);
		nct.start();

		return nct;
	}

	public void setEventSource(WorldWindow newEventSource)
	{
		if (this.eventSource != null)
		{
			this.eventSource.removePositionListener(this);
			this.eventSource.removeRenderingListener(this);
		}

		if (newEventSource != null)
		{
			newEventSource.addPositionListener(this);
			newEventSource.addRenderingListener(this);
		}

		this.eventSource = newEventSource;
	}

	public boolean isShowNetworkStatus()
	{
		return showNetworkStatus.get();
	}

	public void setShowNetworkStatus(boolean showNetworkStatus)
	{
		this.showNetworkStatus.set(showNetworkStatus);
		if (this.netCheckThread != null)
			this.netCheckThread.interrupt();
		this.netCheckThread = showNetworkStatus ? this.startNetCheckThread() : null;
	}

	public void moved(PositionEvent event)
	{
		this.handleCursorPositionChange(event);
	}

	public WorldWindow getEventSource()
	{
		return this.eventSource;
	}

	public String getElevationUnit()
	{
		return this.elevationUnit;
	}

	public void setElevationUnit(String unit)
	{
		if (unit == null)
		{
			String message = Logging.getMessage("nullValue.StringIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		this.elevationUnit = unit;
	}

	public String getAngleFormat()
	{
		return this.angleFormat;
	}

	public void setAngleFormat(String format)
	{
		if (format == null)
		{
			String message = Logging.getMessage("nullValue.StringIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		this.angleFormat = format;
	}

	protected String makeCursorElevationDescription(double metersElevation)
	{
		String s;
		String elev = Logging.getMessage("term.Elev");
		if (UNIT_IMPERIAL.equals(elevationUnit))
			s = String.format(elev + " %,7d feet", (int) (WWMath.convertMetersToFeet(metersElevation)));
		else // Default to metric units.
			s = String.format(elev + " %,7d meters", (int) metersElevation);
		return s;
	}

	protected String makeEyeAltitudeDescription(double metersAltitude)
	{
		String s;
		String altitude = Logging.getMessage("term.Altitude");
		if (UNIT_IMPERIAL.equals(elevationUnit))
			s = String.format(altitude + " %,7d mi", (int) Math.round(WWMath.convertMetersToMiles(metersAltitude)));
		else // Default to metric units.
			s = String.format(altitude + " %,7d m", (int) Math.round(metersAltitude));
		return s;
	}

	protected String makeAngleDescription(String label, Angle angle)
	{
		String s;
		if (Angle.ANGLE_FORMAT_DMS.equals(angleFormat))
			s = String.format("%s %s", label, angle.toDMSString());
		else
			s = String.format("%s %7.4f\u00B0", label, angle.degrees);
		return s;
	}

	protected void handleCursorPositionChange(final PositionEvent event)
	{
		uiHandler.obtainMessage(WHAT_POSITION_CHANGE, event).sendToTarget();
	}

	public void stageChanged(RenderingEvent event)
	{
		if (!event.getStage().equals(RenderingEvent.AFTER_RENDERING))
			return;

		uiHandler.sendEmptyMessage(WHAT_ELEVATION_CHANGE);
	}
}

/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simple.runtime.components.impl.android;

import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import simple.runtime.日志输出;
import simple.runtime.android.MainActivity;
import simple.runtime.android.MainActivity.OnResumeListener;
import simple.runtime.android.MainActivity.OnStopListener;
import simple.runtime.components.位置传感器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import java.io.IOException;
import java.util.List;

/**
 * 可提供经度、纬度和海拔信息的传感器。
 *
 * @author Ellen Spertus
 */
public final class 位置传感器Impl extends 组件Impl
		implements 位置传感器, OnStopListener, OnResumeListener {

	/**
	 * Class that listens for changes in location, raises appropriate events,
	 * and provides properties.
	 *
	 * @author spertus@google.com (Ellen Spertus)
	 */
	private class LocationSensorListener implements LocationListener {
		/**
		 * {@inheritDoc}
		 *
		 * This sets fields longitude, latitude, altitude, hasLocationData, and
		 * hasAltitude, then calls LocationSensor.LocationChanged(), all in the
		 * enclosing class LocationSensor.
		 */
		@Override
		public void onLocationChanged(Location location) {
			lastLocation = location;
			longitude = location.getLongitude();
			latitude = location.getLatitude();
			// If the current location doesn't have altitude information, the prior
			// altitude reading is retained.
			if (location.hasAltitude()) {
				hasAltitude = true;
				altitude = location.getAltitude();
			}
			hasLocationData = true;
			位置改变(latitude, longitude, altitude);
		}

		@Override
		public void onProviderDisabled(String provider) {
			stopListening();
			if (enabled) {
				tryToStartListening();
			}
		}

		@Override
		public void onProviderEnabled(String provider) {
			tryToStartListening();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			switch (status) {
				// Ignore TEMPORARILY_UNAVAILABLE, because service usually returns quickly.
				// case LocationProvider.TEMPORARILY_UNAVAILABLE:
				case LocationProvider.OUT_OF_SERVICE:
					// If the provider we were listening to is no longer available,
					// find another.
					if (provider.equals(providerName)) {
						tryToStartListening();
					}
					break;

				case LocationProvider.AVAILABLE:
					// If another provider becomes available and is one we hadn't known
					// about see if it is better than the one we're currently using.
					if (!provider.equals(providerName) && !allProviders.contains(provider)) {
						tryToStartListening();
					}
					break;
			}
		}
	}

	// Constant returned by Longitude(), Latitude(), and Altitude() if no value could be obtained for
	// them. The client can find this out directly by calling HasLongitudeLatitude() or HasAltitude().
	private static final int UNKNOWN_VALUE = 0;

	// Minimum time in milliseconds between location checks. The documentation for
	// android.location.LocationManager.requestLocationUpdates() does not recommend using a location
	// lower than 60,000 (60 seconds) because of power consumption.
	private static final long MIN_TIME_INTERVAL = 60000;

	// Minimum distance in meters to be reported
	private static final long MIN_DISTANCE_INTERVAL = 5;  // 5 meters

	// These variables contain information related to the LocationProvider.
	private final Criteria locationCriteria;
	private final LocationManager locationManager;

	// These variables are changed together in stopListening() or tryToStartListening().
	private final LocationSensorListener locationSensorListener;
	private List<String> allProviders;  // all providers available when we chose providerName
	private String providerName;
	private LocationProvider locationProvider;
	private boolean listening;

	// These location-related values are set in MyLocationListener.onLocationChanged().
	private double longitude = UNKNOWN_VALUE;
	private double latitude = UNKNOWN_VALUE;
	private double altitude = UNKNOWN_VALUE;
	private Location lastLocation;
	private boolean hasLocationData;
	private boolean hasAltitude;

	// This is used in reverse geocoding.
	private final Geocoder geocoder;

	// This is set to true after Initialize() runs.
	boolean initialized;

	// Backing for properties
	private boolean enabled;

	/**
	 * Creates a new LocationSensor component.
	 *
	 * @param container  container which will hold the component (must not be
	 *                   {@code null}, for non-visible component, like this one
	 *                   must be the form)
	 */
	public 位置传感器Impl(组件容器 container) {
		super(container);

		// Set up stop/resume listeners
		MainActivity context = MainActivity.getContext();
		context.addOnResumeListener(this);
		context.addOnStopListener(this);

		// Initialize location-related fields
		geocoder = new Geocoder(context);
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		locationCriteria = new Criteria();
		locationSensorListener = new LocationSensorListener();
	}

	// 事件

	@Override
	public void 初始化() {
		EventDispatcher.dispatchEvent(this, "初始化");
		initialized = true;

		// 以确保所有监听器都已注册
		启用(启用());
	}

	@Override
	public void 位置改变(double latitude, double longitude, double altitude) {
		if (enabled) {
			EventDispatcher.dispatchEvent(this, "位置改变", latitude, longitude, altitude);
		}
	}

	// 属性

	@Override
	public boolean 是否可用() {
		return hasLocationData;
	}

	@Override
	public boolean 具备海拔() {
		return hasAltitude;
	}

	@Override
	public boolean 具备精度() {
		return 精度() != UNKNOWN_VALUE;
	}

	@Override
	public double 经度() {
		return longitude;
	}

	@Override
	public double 纬度() {
			return latitude;
	}

	@Override
	public double 海拔() {
		return altitude;
	}

	@Override
	public double 精度() {
		if (lastLocation != null && lastLocation.hasAccuracy()) {
			return lastLocation.getAccuracy();
		} else if (locationProvider != null) {
			return locationProvider.getAccuracy();
		} else {
			return UNKNOWN_VALUE;
		}
	}

	@Override
	public boolean 启用() {
		return enabled;
	}

	@Override
	public void 启用(boolean enabled) {
		this.enabled = enabled;
		if (initialized) {
			if (!enabled) {
				stopListening();
			} else {
				tryToStartListening();
			}
		}
	}

	@Override
	public String 当前地址() {
		if (hasLocationData &&
				latitude <= 90 && latitude >= -90 &&
				longitude <= 180 || longitude >= -180) {
			try {
				List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
				if (addresses != null && addresses.size() == 1) {
					Address address = addresses.get(0);
					if (address != null) {
						StringBuilder sb = new StringBuilder();
						for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
							sb.append(address.getAddressLine(i));
							sb.append("\n");
						}
						return sb.toString();
					}
				}
			} catch (IOException e) {
				日志输出.warning("(%d) Exception thrown by getFromLocation() %s", hashCode(), e.getMessage());
			}
		}

		return "";
	}

	// Methods to stop and start listening to LocationProviders

	/**
	 * This unregisters {@link #myLocationListener} as a listener to location
	 * updates.  It is safe to call this even if no listener had been registered,
	 * in which case it has no effect.  This also sets the values of
	 * {@link #providerName}, {@link #locationProvider}, and {@link #allProviders}
	 * to {@code null} and sets {@link #listening} to {@code false}.
	 */
	private void stopListening() {
		if (listening) {
			locationManager.removeUpdates(locationSensorListener);
			providerName = null;
			locationProvider = null;
			allProviders = null;
			listening = false;
		}
	}

	/**
	 * This tries to find the best available location provider and to
	 * register {@link #myLocationListener} as a listener.
	 * <p>
	 * This method is called in two situations, to choose a provider when either
	 * (1) none is currently selected (such as on start/restart or when one
	 * becomes available for the first time), or (2) there is a problem with the
	 * provider currently in use, such as its having been reported no longer
	 * available via onStatusChanged().  To prevent the same poorly
	 * performing provider from being selected, this does not select the current
	 * provider unless it is the only one.
	 *
	 * If this can find and listen to a provider, the private field
	 * {@link #listening} is set to {@code true}, and the variables
	 * {@link #providerName}, {@link #locationProvider}, and {@link
	 * #allProviders}  are given the appropriate values.  Otherwise,
	 * {@link #available} is set to {@code false}, and {@link #providerName},
	 * {@link #locationProvider}, and {@link #allProviders} are set to
	 * {@code null}.
	 */
	private void tryToStartListening() {
		// We may be able to find a provider better than the previous one,
		// if there was one, so stop listening to it.
		String lastProviderName = providerName;
		stopListening();

		// Find the best current provider.
		providerName = locationManager.getBestProvider(locationCriteria, true);
		if (providerName == null || providerName.length() == 0) {
			// No providers are available
			return;
		}

		allProviders = locationManager.getProviders(true);
		// Don't reuse the last provider unless there is no alternative.
		if (providerName.equals(lastProviderName)) {
			if (allProviders == null || allProviders.size() == 0) {
				// No providers are enabled.  This could occur if a provider was
				// disabled between the above calls to getBestProvider() and
				// getProviders().
				providerName = null;
				return;
			}
			// If a different provider can be found, prefer that.
			// Otherwise, just keep using the old one.
			for (String provider : allProviders) {
				if (!provider.equals(lastProviderName)) {
					providerName = provider;
					break;
				}
			}
		}

		// Find the associated LocationProvider.
		locationProvider = locationManager.getProvider(providerName);
		if (locationProvider == null) {
			providerName = null;
			allProviders = null;
			return;
		}

		// Indicate that we are now listening.
		locationManager.requestLocationUpdates(providerName, MIN_TIME_INTERVAL, MIN_DISTANCE_INTERVAL,
				locationSensorListener);
		listening = true;
	}

	// OnResumeListener implementation

	@Override
	public void onResume() {
		if (enabled) {
			tryToStartListening();
		}
	}

	// OnStopListener implementation

	@Override
	public void onStop() {
		stopListening();
	}
}

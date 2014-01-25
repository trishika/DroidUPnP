package org.droidupnp.model.renderer;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import org.droidupnp.R;
import org.droidupnp.view.SettingsActivity;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationError;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.support.avtransport.impl.AVTransportService;
import org.fourthline.cling.support.avtransport.impl.AVTransportStateMachine;
import org.fourthline.cling.support.avtransport.impl.state.AbstractState;
import org.fourthline.cling.support.avtransport.impl.state.NoMediaPresent;
import org.fourthline.cling.support.avtransport.impl.state.Playing;
import org.fourthline.cling.support.avtransport.impl.state.Stopped;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.model.AVTransport;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.seamless.statemachine.States;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public class Renderer
{
	private final static String TAG = "Renderer";

	private UDN udn = null;
	private LocalDevice localDevice = null;
	private LocalService localService = null;
	private Context ctx = null;

	private final static int port = 8192;
	private static InetAddress localAddress;

	public Renderer(InetAddress localAddress, Context ctx) throws ValidationException
	{
		this.ctx = ctx;

		Log.e(TAG, "Creating media server !");

		localService = new AnnotationLocalServiceBinder()
			.read(AVTransportService.class);

		localService.setManager(new DefaultServiceManager<AVTransportService>(
			localService, AVTransportService.class));

		localService.setManager(
			new DefaultServiceManager<AVTransportService>(localService, null) {
				@Override
				protected AVTransportService createServiceInstance() throws Exception {
					return new AVTransportService(
						MyRendererStateMachine.class,   // All states
						MyRendererNoMediaPresent.class  // Initial state
					);
				}
			}
		);

		udn = UDN.valueOf(new UUID(0,10).toString());
		this.localAddress = localAddress;
		this.ctx = ctx;
		createLocalDevice();

		AVTransportService avTransportService = (AVTransportService ) localService.getManager().getImplementation();
//		avTransportService.fireLastChange();

//		avTransportServiceService.setContext(ctx);
//		avTransportServiceService.setBaseURL(getAddress());
	}

	public void createLocalDevice() throws ValidationException
	{
		String version = "";
		try {
			version = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Application version name not found");
		}

		DeviceDetails details = new DeviceDetails(
			SettingsActivity.getSettingContentDirectoryName(ctx),
			new ManufacturerDetails(ctx.getString(R.string.app_name), ctx.getString(R.string.app_url)),
			new ModelDetails(ctx.getString(R.string.app_name), ctx.getString(R.string.app_url)),
			ctx.getString(R.string.app_name), version);

		List<ValidationError> l = details.validate();
		for( ValidationError v : l )
		{
			Log.e(TAG, "Validation pb for property "+ v.getPropertyName());
			Log.e(TAG, "Error is " + v.getMessage());
		}

		DeviceType type = new UDADeviceType("MediaServer", 1);

		localDevice = new LocalDevice(new DeviceIdentity(udn), type, details, localService);
	}

	public LocalDevice getDevice() {
		return localDevice;
	}

	public String getAddress() {
		return localAddress.getHostAddress() + ":" + port;
	}

	@States({
		MyRendererNoMediaPresent.class,
		MyRendererStopped.class,
		MyRendererPlaying.class
	})
	interface MyRendererStateMachine extends AVTransportStateMachine {}

	public class MyRendererNoMediaPresent extends NoMediaPresent {

		public MyRendererNoMediaPresent(AVTransport transport) {
			super(transport);
		}

		@Override
		public Class<? extends AbstractState> setTransportURI(URI uri, String metaData) {

			getTransport().setMediaInfo(
				new MediaInfo(uri.toString(), metaData)
			);

			// If you can, you should find and set the duration of the track here!
			getTransport().setPositionInfo(
				new PositionInfo(1, metaData, uri.toString())
			);

			// It's up to you what "last changes" you want to announce to event listeners
			getTransport().getLastChange().setEventedValue(
				getTransport().getInstanceId(),
				new AVTransportVariable.AVTransportURI(uri),
				new AVTransportVariable.CurrentTrackURI(uri)
			);

			return MyRendererStopped.class;
		}
	}

	public class MyRendererStopped extends Stopped {

		public MyRendererStopped(AVTransport transport) {
			super(transport);
		}

		public void onEntry() {
			super.onEntry();
			// Optional: Stop playing, release resources, etc.
		}

		public void onExit() {
			// Optional: Cleanup etc.
		}

		@Override
		public Class<? extends AbstractState> setTransportURI(URI uri, String metaData) {
			// This operation can be triggered in any state, you should think
			// about how you'd want your player to react. If we are in Stopped
			// state nothing much will happen, except that you have to set
			// the media and position info, just like in MyRendererNoMediaPresent.
			// However, if this would be the MyRendererPlaying state, would you
			// prefer stopping first?
			return MyRendererStopped.class;
		}

		@Override
		public Class<? extends AbstractState> stop() {
			/// Same here, if you are stopped already and someone calls STOP, well...
			return MyRendererStopped.class;
		}

		@Override
		public Class<? extends AbstractState> play(String speed) {
			// It's easier to let this classes' onEntry() method do the work
			return MyRendererPlaying.class;
		}

		@Override
		public Class<? extends AbstractState> next() {
			return MyRendererStopped.class;
		}

		@Override
		public Class<? extends AbstractState> previous() {
			return MyRendererStopped.class;
		}

		@Override
		public Class<? extends AbstractState> seek(SeekMode unit, String target) {
			// Implement seeking with the stream in stopped state!
			return MyRendererStopped.class;
		}
	}

	public class MyRendererPlaying extends Playing {

		public MyRendererPlaying(AVTransport transport) {
			super(transport);
		}

		@Override
		public void onEntry() {
			super.onEntry();
			// Start playing now!
		}

		@Override
		public Class<? extends AbstractState> setTransportURI(URI uri, String metaData) {
			// Your choice of action here, and what the next state is going to be!
			return MyRendererStopped.class;
		}

		@Override
		public Class<? extends AbstractState> stop() {
			// Stop playing!
			return MyRendererStopped.class;
		}

		@Override
		public Class<? extends AbstractState<?>> play(String s) {
			return null;
		}

		@Override
		public Class<? extends AbstractState<?>> pause() {
			return null;
		}

		@Override
		public Class<? extends AbstractState<?>> next() {
			return null;
		}

		@Override
		public Class<? extends AbstractState<?>> previous() {
			return null;
		}

		@Override
		public Class<? extends AbstractState<?>> seek(SeekMode seekMode, String s) {
			return null;
		}
	}
}

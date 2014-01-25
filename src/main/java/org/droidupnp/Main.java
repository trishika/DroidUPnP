package org.droidupnp;

import android.app.Activity;
;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;

import org.droidupnp.controller.upnp.IUpnpServiceController;
import org.droidupnp.model.upnp.IFactory;
import org.droidupnp.view.ContentDirectoryFragment;
import org.droidupnp.view.SettingsActivity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observer;

public class Main extends Activity {

    private static final String TAG = "Main";

    // Controller
    public static IUpnpServiceController upnpServiceController = null;
    public static IFactory factory = null;


    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private DrawerFragment mDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreated : " + savedInstanceState + factory + upnpServiceController);

        // Use cling factory
        if (factory == null)
            factory = new org.droidupnp.controller.cling.Factory();

        // Upnp service
        if (upnpServiceController == null)
            upnpServiceController = factory.createUpnpServiceController(this);

        // Attach listener
        Fragment contentDirectoryFragment = getFragmentManager().findFragmentById(R.id.ContentDirectoryFragment);
        if (contentDirectoryFragment != null && contentDirectoryFragment instanceof Observer)
            upnpServiceController.addSelectedContentDirectoryObserver((Observer) contentDirectoryFragment);
        else
            Log.w(TAG, "No contentDirectoryFragment yet !");

        Fragment rendererFragment = getFragmentManager().findFragmentById(R.id.RendererFragment);
        if (rendererFragment != null && rendererFragment instanceof Observer)
            upnpServiceController.addSelectedRendererObserver((Observer) rendererFragment);
        else
            Log.w(TAG, "No rendererFragment yet !");


        mDrawerFragment = (DrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, new ContentDirectoryFragment())
                .commit();
    }

    @Override
    public void onResume()
    {
        Log.i(TAG, "onResume");
        upnpServiceController.resume(this);
        super.onResume();
    }

    @Override
    public void onPause()
    {
        Log.i(TAG, "onPause");
        upnpServiceController.pause();
        upnpServiceController.getServiceListener().getServiceConnexion().onServiceDisconnected(null);
        super.onPause();
    }

    public void refresh()
    {
        upnpServiceController.getServiceListener().refresh();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.menu_refresh:
                refresh();
                break;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                break;
            case R.id.menu_quit:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    public static InetAddress getLocalIpAddress(Context ctx) throws UnknownHostException
    {
        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return InetAddress.getByName(String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff)));
    }
}
package com.shazvi.azan;
/* TODO:
 * fix persistence-try services
 * start at startup
 * fix fajr azan
 */

import android.app.*;
import android.content.*;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.*;
import android.os.Bundle;
import android.widget.*;
import android.media.MediaPlayer;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Calendar;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MainActivity extends Activity {
	// Global Variables
	public static final int NOTIFICATION_ID = 1;
	public static final String MYTAG = "myappLog";
	public static JSONObject jsontimes;

	public static SharedPreferences sharedPref;
	public static boolean notify_on;
	public static boolean azan_on;
	public static boolean icon_on;
	public static boolean start_on;

	public static String[] prayernames = new String[] {"subah", "sunrise", "dhuhr", "asr", "maghrib", "isha"};
	public static String[] prayertimes = new String[] {"","","","","",""};
	public static String[] azantext = new String[] {"","",""};
    public static MediaPlayer otheraudio = new MediaPlayer();
    public static MediaPlayer fajraudio = new MediaPlayer();
	public static Alarm alarm;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		AssetManager assetManager = getAssets();
		alarm = new Alarm();

		// Local Storage
		sharedPref = this.getPreferences(Context.MODE_PRIVATE);
		notify_on = sharedPref.getBoolean("notify_on", true);
		azan_on = sharedPref.getBoolean("azan_on", true);
		icon_on = sharedPref.getBoolean("icon_on", true);
		start_on = sharedPref.getBoolean("start_on", true);

		// Volume Seekbar onchange Method
		SeekBar volumebar = (SeekBar) findViewById(R.id.volumebar);
		volumebar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fajraudio.setVolume((float) progress / 100, (float) progress / 100);
				otheraudio.setVolume((float) progress / 100, (float) progress / 100);
				sharedPref.edit().putInt("azanvolume", progress).apply();
			}
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}
			@Override public void onStartTrackingTouch(SeekBar seekBar) {}
		});

		// Get JSON file contents
		try {
			InputStream input = assetManager.open("colombo.json");
			int size = input.available();
			byte[] buffer = new byte[size];
			input.read(buffer);
			input.close();
			JSONParser parser = new JSONParser();
			jsontimes = (JSONObject) parser.parse(new String(buffer));
		} catch (FileNotFoundException e) {
			e.getStackTrace();
		} catch (IOException e) {
			e.getStackTrace();
		} catch (ParseException e) {
			e.getStackTrace();
		}

		// Set audio attributes
		try {
            otheraudio.setDataSource(assetManager.openFd("azan.mp3").getFileDescriptor());
            otheraudio.prepare();
            otheraudio.setVolume((float) sharedPref.getInt("azanvolume", 50) / 100, (float) sharedPref.getInt("azanvolume", 50) / 100);

            fajraudio.setDataSource(assetManager.openFd("fajr.mp3").getFileDescriptor());
            fajraudio.prepare();
            fajraudio.setVolume((float) sharedPref.getInt("azanvolume", 50) / 100, (float) sharedPref.getInt("azanvolume", 50) / 100);
		} catch (IOException e) {
			e.getStackTrace();
		}
		((SeekBar) findViewById(R.id.volumebar)).setProgress(sharedPref.getInt("azanvolume", 50));

		alarm.SetAlarm(this.getApplicationContext());
	}

	public class Alarm extends BroadcastReceiver{
		@Override public void onReceive(Context context, Intent intent) {
			Log.w(MYTAG, "Alarm broadcast received");
			PowerManager powermanager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wakelock = powermanager.newWakeLock(PowerManager.FULL_WAKE_LOCK, MYTAG);

			wakelock.acquire();
			//alarm.SetAlarm(MainActivity.this);
			calculate((TextView)findViewById(R.id.mainline1), (TextView)findViewById(R.id.mainline2), MainActivity.this);
			wakelock.release();
		}

		public void SetAlarm(Context context) {
			final String SOME_ACTION = "com.shazvi.azan.MainActivity.Alarm";
			IntentFilter intentFilter = new IntentFilter(SOME_ACTION);
			Alarm mReceiver = new Alarm();
			context.registerReceiver(mReceiver, intentFilter);
			Intent intent = new Intent(SOME_ACTION);
			PendingIntent pendingintent = PendingIntent.getBroadcast(context, 0, intent, 0);

            long nextmin = System.currentTimeMillis()+(60000 - System.currentTimeMillis() % 60000);
            AlarmManager alarmmanager = (AlarmManager)context.getSystemService(ALARM_SERVICE);
			alarmmanager.setRepeating(AlarmManager.RTC_WAKEUP, nextmin, 60000, pendingintent);
			Log.w(MYTAG, "Repeating Alarm has been set");
            calculate((TextView)findViewById(R.id.mainline1), (TextView)findViewById(R.id.mainline2), MainActivity.this);
		}
	}

	@Override protected void onStart() {
		super.onStart();
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_layout, menu);
		// Set onload options
		if (notify_on) menu.findItem(R.id.notifswitch).setChecked(true);
		if (azan_on)  menu.findItem(R.id.azanswitch).setChecked(true);
		if (icon_on)  menu.findItem(R.id.iconswitch).setChecked(true);
		if (start_on)  menu.findItem(R.id.startswitch).setChecked(true);
		return true;
	}

	@Override public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		int i=0;
		for (String prayername : prayernames) {
			int itemId = getResources().getIdentifier(prayername, "id", getPackageName());
			menu.findItem(itemId).setTitle(prayertimes[i]);
			i++;
		}
		return true;
	}

	// Functions for menu items
	public void notifstatechanged(MenuItem item) {
		if (item.isChecked()) {
			notify_on = false;
			sharedPref.edit().putBoolean("notify_on", false).apply();
			clearNotif((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
			item.setChecked(false);
		} else {
			notify_on = true;
			sharedPref.edit().putBoolean("notify_on", true).apply();
			showNotif(MainActivity.this, getResources(), (NotificationManager) getSystemService(NOTIFICATION_SERVICE), false);
			item.setChecked(true);
		}
	}
	public void azanstatechanged(MenuItem item) {
		if (item.isChecked()) {
			azan_on = false;
			sharedPref.edit().putBoolean("azan_on", false).apply();
			item.setChecked(false);
		} else {
			azan_on = true;
			sharedPref.edit().putBoolean("azan_on", true).apply();
			item.setChecked(true);
		}
	}
	public void iconstatechanged(MenuItem item) {
		if (item.isChecked()) {
			icon_on = false;
			sharedPref.edit().putBoolean("icon_on", false).apply();
			clearNotif((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
			if(notify_on) showNotif(MainActivity.this, getResources(), (NotificationManager)getSystemService(NOTIFICATION_SERVICE), false);
			item.setChecked(false);
		} else {
			icon_on = true;
			sharedPref.edit().putBoolean("icon_on", true).apply();
			clearNotif((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
			if(notify_on) showNotif(MainActivity.this, getResources(), (NotificationManager)getSystemService(NOTIFICATION_SERVICE), false);
			item.setChecked(true);
		}
	}
	public void startstatechanged(MenuItem item) {
		if (item.isChecked()) {
			start_on = false;
			sharedPref.edit().putBoolean("start_on", false).apply();
            // todo
			item.setChecked(false);
		} else {
			start_on = true;
			sharedPref.edit().putBoolean("start_on", true).apply();
			// todo
			item.setChecked(true);
		}
	}

	// Otheraudio play method for button
	public void playotheraudio(View view) {
		if(!otheraudio.isPlaying()) {
			otheraudio.seekTo(0);
			otheraudio.start();
		}
	}

	// Audio Pause Method
	public void pauseaudio(View view) {
		if(otheraudio.isPlaying()) {
			otheraudio.pause();
		}
		if(fajraudio.isPlaying()) {
			fajraudio.pause();
		}

        if(azantext[1].startsWith("Tap")) {
            TextView t2= (TextView) findViewById(R.id.mainline2);
            t2.setText("");
        }
	}

	// Azan time calculation method. Returns next prayer and time remaining
	public static void calculate(TextView t1, TextView t2, MainActivity mthis) {
		Log.w(MYTAG, "calculate() triggered");
		// Variables
		Boolean done = false;
		Boolean ticker = false;
		Calendar now = Calendar.getInstance();
		/* adjust time for debugging */ //now.setTimeInMillis(now.getTimeInMillis()-(22*60000));
		Long offset = (Long) jsontimes.get("offset");
		now.setTimeInMillis( now.getTimeInMillis() - now.get(Calendar.ZONE_OFFSET) + offset );

		JSONObject thismonth = (JSONObject) jsontimes.get(Integer.toString(now.get(Calendar.MONTH)));
		JSONObject today = (JSONObject) thismonth.get(Integer.toString(now.get(Calendar.DATE)));

		azantext[2] = "";

		// Loop through current day's prayer times
		int i=0;
		for (String prayername : prayernames) {
			prayertimes[i] = ucfirst(prayername)+": "+hr12(today.get(prayername).toString());

			if (!done) {
				Calendar praytime = Calendar.getInstance();
				praytime.set( Calendar.HOUR_OF_DAY, Integer.parseInt(today.get(prayername).toString().split(":")[0]));
				praytime.set( Calendar.MINUTE, Integer.parseInt(today.get(prayername).toString().split(":")[1]));
				MediaPlayer audio = prayername.equals("subah")?fajraudio:otheraudio;
				long diff;

				if(praytime.getTimeInMillis() > now.getTimeInMillis()) { // if prayer is after now
					diff = (praytime.getTimeInMillis() - now.getTimeInMillis())/1000; // difference in seconds
					if(diff >= 60) { // diff is 1 minute or higher
						azantext[0] = ucfirst(prayername)+" is at "+hr12(today.get(prayername).toString());
						azantext[1] = timeremain(diff) +" rem.";

						if(Math.round(diff/60)==20) {
							azantext[2] = "Azan is in 20 minutes";
							ticker = true;
						}
					} else { // diff is less than 1 minute
						azantext[0] = ucfirst(prayername)+" is right now";
						azantext[2] = ucfirst(prayername)+" azan is right now";
						if(azan_on && !audio.isPlaying() && !prayername.equals("sunrise")) { // Play audio
							audio.seekTo(0);
							audio.start();
                            Log.w(MYTAG, "Time for "+prayername);
						}
						azantext[1] = audio.isPlaying()?"Tap here to mute Azan":"";
                        ticker = true;
					}
					done = true;
				} else if(prayername.equals("isha")) { // between Isha and 12:00(ie. nextprayer is tomorrow's Subah)
					praytime.set(Calendar.DATE, praytime.get(Calendar.DATE)+1);
					JSONObject tommonth = (JSONObject) jsontimes.get(Integer.toString(praytime.get(Calendar.MONTH)));
					JSONObject tommorow = (JSONObject) tommonth.get(Integer.toString(praytime.get(Calendar.DATE)));
					praytime.set( Calendar.HOUR_OF_DAY, Integer.parseInt(tommorow.get("subah").toString().split(":")[0]));
					praytime.set( Calendar.MINUTE, Integer.parseInt(tommorow.get("subah").toString().split(":")[1]));

					diff = (praytime.getTimeInMillis() - now.getTimeInMillis())/1000; // tomorrow's subah minus now(), in seconds
					azantext[0] = "Subah is at "+hr12(tommorow.get("subah").toString())+" tomorrow";
					azantext[1] = timeremain(diff) +" rem.";
					done=true;
				}
			}
			i++;
		}

		// Update app main page time values
		t1.setText(azantext[0]);
		t2.setText(azantext[1]);
		if(notify_on) {
			showNotif(mthis, mthis.getResources(), (NotificationManager) mthis.getSystemService(NOTIFICATION_SERVICE), ticker);
		}
	}

	public static String hr12(String time) {
		int hr = Integer.parseInt(time.split(":")[0]);
		String min = time.split(":")[1];
		String apm = (hr>11)?"pm":"am";
		hr %= 12;
		if(hr==0)hr=12;
		return Integer.toString(hr)+":"+min+" "+apm;
	}
	private static String timeremain(long difference) {
		int min = Math.round(difference/60)%60;
		String minval;
		if(min==1)minval = "1 minute";
		else if(min==0) minval = "";
		else minval = Integer.toString(min)+" minutes";

		int hr = (int) difference/60/60;
		String hrval;
		if(hr==1) hrval = "1 hour";
		else if(hr==0) hrval = "";
		else hrval = Integer.toString(hr)+" hours";

		String and = (hr!=0 && min!=0)?" and ":"";
		return hrval+and+minval;
	}
	public static String ucfirst(String line) { return Character.toUpperCase(line.charAt(0)) + line.substring(1); }

	public static void showNotif(MainActivity thisis, Resources thisres, NotificationManager thisSer, boolean tickert) {
		Intent intent = new Intent(thisis, MainActivity.class);
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent pendingIntent = PendingIntent.getActivity(thisis, 0, intent, 0);

		Notification.Builder builder = new Notification.Builder(thisis);
		builder.setContentIntent(pendingIntent);
		builder.setSmallIcon(R.drawable.ic_stat_notification);
		builder.setOngoing(true);
		builder.setAutoCancel(false);
		if(!icon_on) {
            builder.setPriority(Notification.PRIORITY_MIN);
        }
        if(tickert) {
            builder.setTicker(azantext[2]);
        }
		builder.setLargeIcon(BitmapFactory.decodeResource(thisres, R.drawable.ic_launcher));

		builder.setContentTitle("Azan");
		builder.setContentText(azantext[0]);
		builder.setSubText(azantext[1]);
		thisSer.notify(NOTIFICATION_ID, builder.build());
	}

	public static void clearNotif(NotificationManager thisSer) {
		thisSer.cancel(NOTIFICATION_ID);
	}
}
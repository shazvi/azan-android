// Notification Switch onChange Method
Switch notifSwitch = (Switch) findViewById(R.id.notifswitch);
notifSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
    @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        if (isChecked) {
            notify_on = true;
            sharedPref.edit().putBoolean("notify_on", true).apply();
            showNotif(findViewById(R.id.notifswitch));
        } else {
            notify_on = false;
            sharedPref.edit().putBoolean("notify_on", false).apply();
            clearNotif(findViewById(R.id.notifswitch));
        }
    }
});

// Azan Switch onChange Method
Switch azanSwitch = (Switch) findViewById(R.id.azanswitch);
azanSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        if (isChecked) {
            azan_on = true;
            sharedPref.edit().putBoolean("azan_on", true).apply();
        } else {
            azan_on = false;
            sharedPref.edit().putBoolean("azan_on", false).apply();
        }
    }
});

// Icon Switch onChange Method
Switch iconSwitch = (Switch) findViewById(R.id.iconswitch);
iconSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
    @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        if (isChecked) {
            icon_on = true;
            sharedPref.edit().putBoolean("icon_on", true).apply();
            clearNotif(findViewById(R.id.iconswitch));
            if (notify_on) showNotif(findViewById(R.id.iconswitch));
        } else {
            icon_on = false;
            sharedPref.edit().putBoolean("icon_on", false).apply();
            clearNotif(findViewById(R.id.iconswitch));
            if (notify_on) showNotif(findViewById(R.id.iconswitch));
        }
    }
});
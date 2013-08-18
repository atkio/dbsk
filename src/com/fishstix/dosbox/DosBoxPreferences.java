/*
 *  Copyright (C) 2012 Fishstix (Gene Ruebsamen - ruebsamen.gene@gmail.com)
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package com.fishstix.dosbox;

import name.atkio.dev.android.dosbox.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class DosBoxPreferences extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener {
	private Preference doscpu = null;
    private Preference doscycles = null;
    private Preference dosframeskip = null;
    private Preference dosmemsize = null;
    private Preference dossbtype = null;
    private Preference dosmanual = null;
    private Preference dosautoexec = null;
   // private Preference confmousetapclick = null;
    private Preference confinputmode = null;
    private Preference confbuttonoverlay = null;
    private Preference confjoyoverlay = null;
    private Preference confenabledpad = null;
    private Preference confdpadsensitivity = null;
    private Preference confmousetracking = null;
    private Preference confturbomixer = null;
    private Preference confsound = null;
    private Preference dosautocpu = null;
   // private Preference confabsolutecalibrate = null;
   // private Preference confabsolutedefault = null;
    private Context ctx = null;
    
    private static final int TOUCHSCREEN_MOUSE = 0;
    private static final int TOUCHSCREEN_JOY = 1;
    private static final int PHYSICAL_MOUSE = 2;
    private static final int PHYSICAL_JOY = 3;
    private static final int SCROLL_SCREEN = 4;
    
	  @Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    ctx = this;
	    
	    addPreferencesFromResource(R.xml.preferences);
	    doscpu = (Preference) findPreference("doscpu");
	    doscycles = (Preference) findPreference("doscycles");
	    dosframeskip = (Preference) findPreference("dosframeskip");
	    dosmemsize = (Preference) findPreference("dosmemsize");
	    dossbtype = (Preference) findPreference("dossbtype");
	    dosmanual = (Preference) findPreference("dosmanualconf");
	    //confmousetapclick = (Preference) findPreference("confmousetapclick");
	    confinputmode = (Preference) findPreference("confinputmode");
	    confbuttonoverlay = (Preference) findPreference("confbuttonoverlay");
	    confjoyoverlay = (Preference) findPreference("confjoyoverlay");
	    confenabledpad = (Preference) findPreference("confenabledpad");
	    confdpadsensitivity = (Preference) findPreference("confdpadsensitivity");
	    confmousetracking = (Preference) findPreference("confmousetracking");
	    confturbomixer = (Preference) findPreference("confturbomixer");
	    confsound = (Preference) findPreference("confsound");
	    dosautoexec = (Preference) findPreference("dosautoexec");
	    dosautocpu = (Preference) findPreference("dosautocpu");
	   // confabsolutecalibrate = (Preference) findPreference("confabsolutecalibrate");
	   // confabsolutedefault = (Preference) findPreference("confabsolutedefault");
	    
	    doscpu.setOnPreferenceChangeListener(this);
	    doscycles.setOnPreferenceChangeListener(this);
	    dosframeskip.setOnPreferenceChangeListener(this);
	    dosmemsize.setOnPreferenceChangeListener(this);
	    dosmanual.setOnPreferenceChangeListener(this);
	    dossbtype.setOnPreferenceChangeListener(this);
	    confinputmode.setOnPreferenceChangeListener(this);
	    //confbuttonoverlay.setOnPreferenceChangeListener(this);
	    confenabledpad.setOnPreferenceChangeListener(this);
	    confturbomixer.setOnPreferenceChangeListener(this);
	    confsound.setOnPreferenceChangeListener(this);
	    dosautoexec.setOnPreferenceChangeListener(this);
	    dosautocpu.setOnPreferenceChangeListener(this);
	    // confabsolutecalibrate.setOnPreferenceClickListener(this);
	   // confabsolutedefault.setOnPreferenceClickListener(this);

		if (dosmanual.getSharedPreferences().getBoolean("dosmanualconf", false)) {
			doscpu.setEnabled(false);
			doscycles.setEnabled(false);
			dosframeskip.setEnabled(false);
			dosmemsize.setEnabled(false);
			dossbtype.setEnabled(false);
			dosautoexec.setEnabled(false);
		} else {
			doscpu.setEnabled(true);
			doscycles.setEnabled(true);
			dosframeskip.setEnabled(true);
			dosmemsize.setEnabled(true);
			dossbtype.setEnabled(true);
			dosautoexec.setEnabled(true);
		}
		
		// enable/disable settings based upon input mode
		configureInputSettings(Integer.valueOf(confinputmode.getSharedPreferences().getString("confinputmode", "0")));

		// disable dpad sensitivity when dpad is not enabled
		if (confenabledpad.getSharedPreferences().getBoolean("confenabledpad",false)) {
			confdpadsensitivity.setEnabled(true);
		} else {
			confdpadsensitivity.setEnabled(false);
		}
		
	  
	    // get the two custom preferences
	    Preference versionPref = (Preference) findPreference("version");
	    Preference helpPref = (Preference) findPreference("help");
	    //helpPref.setOnPreferenceClickListener(this);
	    String versionName="";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    versionPref.setSummary(versionName);
	  }

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference == dosmanual) {
			// disable dosbox configuration
			if (newValue.toString().equals("true")) {
				doscpu.setEnabled(false);
				doscycles.setEnabled(false);
				dosframeskip.setEnabled(false);
				dosmemsize.setEnabled(false);
				dosautoexec.setEnabled(false);
				Toast.makeText(ctx, R.string.manual, Toast.LENGTH_SHORT).show();
			} else {
				doscpu.setEnabled(true);
				doscycles.setEnabled(true);
				dosframeskip.setEnabled(true);
				dosmemsize.setEnabled(true);
				dosautoexec.setEnabled(true);
				Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();
			}
		} else if (preference == confinputmode) {
			
			configureInputSettings(Integer.valueOf(newValue.toString()));
			
	/*	} else if (preference == confbuttonoverlay) {
			if (newValue.toString().equals("true")) {
				// disable click screen on button overlay
				confmousetapclick.getEditor().putBoolean("confmousetapclick", false).commit();
				confmousetapclick.setEnabled(false);				
			} else {
				confmousetapclick.setEnabled(true);			
			} */
		} else if (preference == confenabledpad) {
			if (newValue.toString().equals("true")) {
				confdpadsensitivity.setEnabled(true);
			} else {
				confdpadsensitivity.setEnabled(false);
			}
		} else {
			Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();
		}
		return true;
	}

	public boolean onPreferenceClick(Preference preference) {
		/*if (preference == confabsolutecalibrate) {
			// calibrate absolute mode
			confabsolutecalibrate.getEditor().putBoolean("conf_doReset", false);
			confabsolutecalibrate.getEditor().putBoolean("conf_doCalibrate", true).commit();
		} else if (preference == confabsolutedefault) {
			// reset defaults
			confabsolutecalibrate.getEditor().putBoolean("conf_doReset", true);
			confabsolutecalibrate.getEditor().putBoolean("conf_doCalibrate", false).commit();	
			Toast.makeText(ctx, R.string.absdefault, Toast.LENGTH_SHORT).show();
		}*/
		//getParent().getApplicationContext().startActivity(new Intent(getParent().getApplicationContext(), DosBoxHelp.class));
		return false;
	}
	
	private void configureInputSettings(int input_mode) {
		switch (input_mode) {
		case TOUCHSCREEN_MOUSE:
			// enable tracking settings
			confmousetracking.setEnabled(true);
			confbuttonoverlay.setEnabled(true);
			confjoyoverlay.setEnabled(false);
			confjoyoverlay.getEditor().putBoolean("confjoyoverlay", false).commit();
			break;
		case TOUCHSCREEN_JOY:
			confmousetracking.setEnabled(false);
			confbuttonoverlay.setEnabled(false);
			confjoyoverlay.setEnabled(true);
			confbuttonoverlay.getEditor().putBoolean("confbuttonoverlay", false).commit();
			break;
		case PHYSICAL_MOUSE:
			confmousetracking.setEnabled(true);
			confbuttonoverlay.setEnabled(false);
			confjoyoverlay.setEnabled(false);
			confjoyoverlay.getEditor().putBoolean("confjoyoverlay", false);
			confbuttonoverlay.getEditor().putBoolean("confbuttonoverlay", false).commit();
			break;
		case PHYSICAL_JOY:
		case SCROLL_SCREEN:
			confmousetracking.setEnabled(false);
			confbuttonoverlay.setEnabled(false);
			confjoyoverlay.setEnabled(false);
			confjoyoverlay.getEditor().putBoolean("confjoyoverlay", false);
			confbuttonoverlay.getEditor().putBoolean("confbuttonoverlay", false).commit();
			break;		
		}		
	}
}

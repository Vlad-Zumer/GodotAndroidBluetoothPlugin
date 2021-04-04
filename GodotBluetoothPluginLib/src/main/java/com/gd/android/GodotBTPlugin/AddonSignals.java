package com.gd.android.GodotBTPlugin;

import org.godotengine.godot.plugin.SignalInfo;

public class AddonSignals
{
    static final class AddonSignalsNames
    {
        static final String LOG_SIGNAL_NAME = "";
        static final String ERROR_SIGNAL_NAME = "";
    }


    static final SignalInfo LOG_SIGNAL = new SignalInfo(AddonSignalsNames.LOG_SIGNAL_NAME, String.class);

    static final SignalInfo ERROR_SIGNAL = new SignalInfo(AddonSignalsNames.ERROR_SIGNAL_NAME);

}

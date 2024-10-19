package com.converter.image2sketch.ui.theme;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import androidx.annotation.RequiresApi;

import com.converter.image2sketch.R;

class ThemeManager {

    public static void applyTheme(Activity activity, boolean dynamicColor) {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyDynamicTheme(activity);
        } else {
            applyStandardTheme(activity);
        }
    }

    private static void applyStandardTheme(Activity activity) {
        if (isDarkTheme(activity)) {
            activity.setTheme(R.style.Theme_Image2Sketch);
        } else {
            activity.setTheme(R.style.Theme_Image2Sketch);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static void applyDynamicTheme(Activity activity) {
        // Dynamic theme logic can go here if using Material You dynamic color (Android 12+)
        applyStandardTheme(activity);
    }

    private static boolean isDarkTheme(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}

package org.elsys.checksplitter.tools;

//For android APK >= 23

import android.app.Activity;
import android.content.Context;

public interface RequestPermissionsTool {
    void requestPermissions(Activity context, String[] permissions);

    boolean isPermissionGranted(Context context, String[] permissions);

    void onPermissionDenied();

}

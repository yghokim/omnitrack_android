package com.github.javiersantos.appupdater;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.PreferenceManager;

import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.net.URL;
import java.util.function.Consumer;

/**
 * Click listener for the "Update" button of the update dialog. <br/>
 * Extend this class to add custom actions to the button on top of the default functionality.
 */
public class UpdateClickListener implements DialogInterface.OnClickListener {

    private final Context context;
    private final UpdateFrom updateFrom;
    private final URL apk;
    private final String appId;
    private final int iconResId;

    public UpdateClickListener(final Context context, final UpdateFrom updateFrom, final URL apk, final String appId, final int iconResId) {
        this.context = context;
        this.updateFrom = updateFrom;
        this.apk = apk;
        this.appId = appId;
        this.iconResId = iconResId;
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        if (context instanceof Activity) {
            RxPermissions permissions = new RxPermissions((Activity) context);
            permissions.request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe(new io.reactivex.functions.Consumer<Boolean>() {
                                   @Override
                                   public void accept(Boolean granted) throws Exception {
                                       if (granted) {
                                           UtilsLibrary.goToUpdate(context, updateFrom, apk, appId, iconResId);
                                       }
                                   }
                               }
                        );
        }
    }
}

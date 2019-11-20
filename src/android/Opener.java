/*
 Copyright 2019 Selerix Systems Inc.

 Licensed under MIT.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
*/

package com.selerix.cordova.plugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;

import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Browser;
import android.os.Parcelable;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

@TargetApi(19)
public class Opener extends CordovaPlugin {

    private static final String LOG_TAG = "Opener";
    private static final String MESSAGE_TASK = "Cordova Android Opener.open() called.";
    private static final String MESSAGE_ERROR = "Error while opening url ";

    @Override
    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext)
            throws JSONException {
        if (action.equals("open")) {

            //this.callbackContext = callbackContext;
            final String url = args.getString(0);

            LOG.d(LOG_TAG, MESSAGE_TASK);
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String result = "";
                    result = open(url);
                    final PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                }
            });
        } else {
            return false;
        }
        return true;
    }

    public String open(final String url) {
        try {
            Intent intent = null;
            intent = new Intent(Intent.ACTION_VIEW);
            // Omitting the MIME type for file: URLs causes "No Activity found to handle
            // Intent".
            // Adding the MIME type to http: URLs causes them to not be handled by the
            // downloader.
            final Uri uri = Uri.parse(url);
            if ("file".equals(uri.getScheme())) {
                intent.setDataAndType(uri, webView.getResourceApi().getMimeType(uri));
            } else {
                intent.setData(uri);
            }
            String currentPackage = cordova.getActivity().getPackageName();
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, currentPackage);

            // Preventing from opening in the current app            
            boolean hasCurrentPackage = false;

            PackageManager pm = cordova.getActivity().getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
            ArrayList<Intent> targetIntents = new ArrayList<Intent>();

            for (ResolveInfo ri : activities) {
                if (!currentPackage.equals(ri.activityInfo.packageName)) {
                    Intent targetIntent = (Intent)intent.clone();
                    targetIntent.setPackage(ri.activityInfo.packageName);
                    targetIntents.add(targetIntent);
                }
                else {
                    hasCurrentPackage = true;
                }
            }

            // If the current app package isn't a target for this URL, then use
            // the normal launch behavior
            if (hasCurrentPackage == false || targetIntents.size() == 0) {
                this.cordova.getActivity().startActivity(intent);
            }
            // If there's only one possible intent, launch it directly
            else if (targetIntents.size() == 1) {
                this.cordova.getActivity().startActivity(targetIntents.get(0));
            }
            // Otherwise, show a custom chooser without the current app listed
            else if (targetIntents.size() > 0) {
                Intent chooser = Intent.createChooser(targetIntents.remove(targetIntents.size()-1), null);
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toArray(new Parcelable[] {}));
                this.cordova.getActivity().startActivity(chooser);
            }
            return "";
            // not catching FileUriExposedException explicitly because buildtools<24 doesn't
            // know about it
        } catch (final java.lang.RuntimeException e) {
            LOG.d(LOG_TAG, MESSAGE_ERROR + url + ":"+ e.toString());
            return e.toString();
        }
    }
}

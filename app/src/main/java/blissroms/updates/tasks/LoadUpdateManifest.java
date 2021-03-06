/*
 * Copyright (C) 2017 Nicholas Chum (nicholaschum) and Matt Booth (Kryten2k35).
 *
 * Licensed under the Attribution-NonCommercial-ShareAlike 4.0 International 
 * (the "License") you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://creativecommons.org/licenses/by-nc-sa/4.0/legalcode
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package blissroms.updates.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import blissroms.updates.R;
import blissroms.updates.utils.Constants;
import blissroms.updates.utils.Utils;

public class LoadUpdateManifest extends AsyncTask<Void, Void, Void> implements Constants {

    private static final String MANIFEST = "update_manifest.xml";
    public final String TAG = this.getClass().getSimpleName();
    // Did this come from the BackgroundReceiver class?
    private boolean shouldUpdateForegroundApp;
    private Context mContext;
    private ProgressDialog mLoadingDialog;

    public LoadUpdateManifest(Context context, boolean input) {
        mContext = context;
        shouldUpdateForegroundApp = input;
    }

    @Override
    protected void onPreExecute() {
        if (shouldUpdateForegroundApp) {
            mLoadingDialog = new ProgressDialog(mContext);
            mLoadingDialog.setIndeterminate(true);
            mLoadingDialog.setCancelable(false);
            mLoadingDialog.setMessage(mContext.getResources().getString(R.string.loading));
            mLoadingDialog.show();
        }

        File manifest = new File(mContext.getFilesDir().getPath(), MANIFEST);
        if (manifest.exists()) {
            boolean deleted = manifest.delete();
            if (!deleted) Log.e(TAG, "Could not delete manifest file...");
        }
    }

    @Override
    protected Void doInBackground(Void... v) {

        try {
            InputStream input;

            URL url = new URL(Utils.getProp("ro.ota.manifest"));
            URLConnection connection = url.openConnection();
            connection.connect();
            // download the file
            input = new BufferedInputStream(url.openStream());

            OutputStream output = mContext.openFileOutput(
                    MANIFEST, Context.MODE_PRIVATE);

            byte data[] = new byte[1024];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            // file finished downloading, parse it!
            RomXmlParser parser = new RomXmlParser();
            parser.parse(new File(mContext.getFilesDir(), MANIFEST),
                    mContext);
        } catch (Exception e) {
            Log.d(TAG, "Exception: " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        Intent intent;
        if (shouldUpdateForegroundApp) {
            mLoadingDialog.cancel();
            intent = new Intent(MANIFEST_LOADED);
        } else {
            intent = new Intent(MANIFEST_CHECK_BACKGROUND);
        }

        mContext.sendBroadcast(intent);
        super.onPostExecute(result);
    }
}
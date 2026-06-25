package com.snapjar.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import com.getcapacitor.BridgeActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(FolderAccessPlugin.class);
        registerPlugin(IncomingFilePlugin.class);
        super.onCreate(savedInstanceState);
        handleIncoming(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncoming(intent);
        // app already open: nudge the web layer to pick up the pending file
        try {
            if (getBridge() != null && getBridge().getWebView() != null) {
                getBridge().getWebView().post(() ->
                    getBridge().getWebView().evaluateJavascript(
                        "window.dispatchEvent(new Event('sjIncomingFile'))", null));
            }
        } catch (Exception ignored) {}
    }

    /** Copy an incoming VIEW/SEND file (PDF or image) into app storage for the web layer. */
    private void handleIncoming(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Uri uri = null;
        if (Intent.ACTION_VIEW.equals(action)) {
            uri = intent.getData();
        } else if (Intent.ACTION_SEND.equals(action)) {
            uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        if (uri == null) return;
        try {
            String mime = getContentResolver().getType(uri);
            String name = resolveName(uri, mime);
            File dir = new File(getFilesDir(), "incoming");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, name);
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) return;
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
            fos.close();
            in.close();
            IncomingFilePlugin.pendingPath = out.getAbsolutePath();
            IncomingFilePlugin.pendingName = name;
            IncomingFilePlugin.pendingMime = mime == null ? "application/octet-stream" : mime;
        } catch (Exception ignored) {}
    }

    private String resolveName(Uri uri, String mime) {
        String name = null;
        try {
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0 && c.moveToFirst()) name = c.getString(idx);
                c.close();
            }
        } catch (Exception ignored) {}
        if (name == null || name.length() == 0) {
            name = uri.getLastPathSegment();
            if (name == null) name = "document";
        }
        if (!name.contains(".")) {
            if (mime != null && mime.contains("pdf")) name += ".pdf";
            else if (mime != null && mime.startsWith("image/")) name += "." + mime.substring(6);
        }
        return name.replaceAll("[\\\\/]", "_");
    }
}

package com.snapjar.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;

import androidx.activity.result.ActivityResult;
import androidx.documentfile.provider.DocumentFile;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Play-safe folder access via the Storage Access Framework. Lets the user grant ONE
 * folder (persisted), list the PDFs inside it, and read a chosen PDF's bytes — all with
 * no broad storage permission, so the Play listing stays approvable.
 */
@CapacitorPlugin(name = "FolderAccess")
public class FolderAccessPlugin extends Plugin {

    @PluginMethod
    public void pickFolder(PluginCall call) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(call, intent, "folderResult");
    }

    @ActivityCallback
    private void folderResult(PluginCall call, ActivityResult result) {
        if (call == null) return;
        Intent data = result.getData();
        if (result.getResultCode() != Activity.RESULT_OK || data == null || data.getData() == null) {
            call.reject("cancelled");
            return;
        }
        Uri treeUri = data.getData();
        try {
            getContext().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        DocumentFile root = DocumentFile.fromTreeUri(getContext(), treeUri);
        JSObject ret = new JSObject();
        ret.put("uri", treeUri.toString());
        ret.put("name", root != null && root.getName() != null ? root.getName() : "Folder");
        call.resolve(ret);
    }

    @PluginMethod
    public void listPdfs(PluginCall call) {
        String uriStr = call.getString("uri");
        if (uriStr == null) { call.reject("no uri"); return; }
        try {
            Uri treeUri = Uri.parse(uriStr);
            DocumentFile root = DocumentFile.fromTreeUri(getContext(), treeUri);
            JSArray arr = new JSArray();
            if (root != null && root.canRead()) collect(root, arr, 0, new int[]{0});
            JSObject ret = new JSObject();
            ret.put("files", arr);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject(e.getMessage() == null ? "list failed" : e.getMessage());
        }
    }

    private void collect(DocumentFile dir, JSArray arr, int depth, int[] count) {
        if (depth > 4 || count[0] > 500) return;          // guard runaway trees
        DocumentFile[] kids = dir.listFiles();
        for (DocumentFile f : kids) {
            if (count[0] > 500) return;
            if (f.isDirectory()) {
                collect(f, arr, depth + 1, count);
            } else {
                String name = f.getName();
                String type = f.getType();
                boolean isPdf = (name != null && name.toLowerCase().endsWith(".pdf")) || "application/pdf".equals(type);
                if (isPdf) {
                    JSObject o = new JSObject();
                    o.put("name", name != null ? name : "Document.pdf");
                    o.put("uri", f.getUri().toString());
                    o.put("size", f.length());
                    o.put("modified", f.lastModified());
                    arr.put(o);
                    count[0]++;
                }
            }
        }
    }

    @PluginMethod
    public void readFile(PluginCall call) {
        String uriStr = call.getString("uri");
        if (uriStr == null) { call.reject("no uri"); return; }
        InputStream is = null;
        try {
            Uri uri = Uri.parse(uriStr);
            is = getContext().getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            JSObject ret = new JSObject();
            ret.put("data", b64);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject(e.getMessage() == null ? "read failed" : e.getMessage());
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
        }
    }
}

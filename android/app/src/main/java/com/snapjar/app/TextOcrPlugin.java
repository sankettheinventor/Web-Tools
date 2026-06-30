package com.snapjar.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

/**
 * Native ML Kit OCR (Latin script) for Scan-to-Text.
 *
 * tesseract.js hangs / reloads the WebView, so OCR runs natively instead: fast (<1s),
 * accurate, fully offline. JS calls recognize({ path }) for a scanned/captured file URI,
 * or recognize({ base64 }) for a gallery image, and gets back { text }.
 */
@CapacitorPlugin(name = "TextOcr")
public class TextOcrPlugin extends Plugin {

    @PluginMethod
    public void recognize(final PluginCall call) {
        final String path = call.getString("path");
        final String base64 = call.getString("base64");
        try {
            InputImage image;
            if (base64 != null && base64.length() > 0) {
                String b = base64;
                int comma = b.indexOf(',');
                if (b.startsWith("data:") && comma >= 0) b = b.substring(comma + 1);   // strip data-URL prefix
                byte[] bytes = Base64.decode(b, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp == null) { call.reject("Could not decode the image"); return; }
                image = InputImage.fromBitmap(bmp, 0);
            } else if (path != null && path.length() > 0) {
                image = InputImage.fromFilePath(getContext(), Uri.parse(path));
            } else {
                call.reject("path or base64 required");
                return;
            }
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer
                .process(image)
                .addOnSuccessListener(result -> {
                    // Reconstruct clean paragraphs: ML Kit's getText() puts a line break after EVERY
                    // detected line (choppy). Instead join wrapped lines within a block into one
                    // paragraph (de-hyphenating line-wrap hyphens), and separate blocks with a blank line.
                    StringBuilder sb = new StringBuilder();
                    for (com.google.mlkit.vision.text.Text.TextBlock block : result.getTextBlocks()) {
                        StringBuilder para = new StringBuilder();
                        for (com.google.mlkit.vision.text.Text.Line line : block.getLines()) {
                            String lt = line.getText() == null ? "" : line.getText().trim();
                            if (lt.length() == 0) continue;
                            if (para.length() > 0) {
                                if (para.charAt(para.length() - 1) == '-') para.setLength(para.length() - 1); // join hyphenated word
                                else para.append(' ');
                            }
                            para.append(lt);
                        }
                        if (para.length() == 0) continue;
                        if (sb.length() > 0) sb.append("\n\n");
                        sb.append(para);
                    }
                    String text = sb.length() > 0 ? sb.toString() : result.getText();
                    JSObject o = new JSObject();
                    o.put("text", text);
                    call.resolve(o);
                })
                .addOnFailureListener(e -> call.reject(e.getMessage() == null ? "OCR failed" : e.getMessage()));
        } catch (Throwable t) {
            call.reject(t.getMessage() == null ? "OCR error" : t.getMessage());
        }
    }
}

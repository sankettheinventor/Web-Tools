package com.snapjar.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

/**
 * App Open ad bridge.
 *
 * The @capacitor-community/admob plugin (used for banner + interstitial) has NO App Open
 * support, so this small additive plugin drives App Open ads directly through the GMA SDK
 * that admob already bundles (play-services-ads). It does NOT touch banner/interstitial.
 *
 * JS contract (exposed as the "AppOpenAd" Capacitor plugin):
 *   load({ adId })   -> requests + preloads one App Open ad
 *   isLoaded()       -> { loaded: boolean }
 *   show()           -> { shown: boolean }   (shows the preloaded ad if any)
 * Events: appOpenLoaded, appOpenFailedToLoad, appOpenShowed, appOpenDismissed, appOpenFailedToShow
 */
@CapacitorPlugin(name = "AppOpenAd")
public class AppOpenPlugin extends Plugin {
    private AppOpenAd ad = null;
    private boolean loading = false;
    private boolean showing = false;

    @PluginMethod
    public void load(final PluginCall call) {
        final String adId = call.getString("adId");
        if (adId == null || adId.length() == 0) { call.reject("adId required"); return; }
        if (ad != null || loading) { call.resolve(); return; }   // already have / fetching one
        loading = true;
        final AppOpenPlugin self = this;
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                try {
                    AdRequest req = new AdRequest.Builder().build();
                    AppOpenAd.load(getContext(), adId, req, new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(AppOpenAd loaded) {
                            ad = loaded; loading = false;
                            self.notifyListeners("appOpenLoaded", new JSObject());
                        }
                        @Override
                        public void onAdFailedToLoad(LoadAdError err) {
                            ad = null; loading = false;
                            JSObject o = new JSObject();
                            o.put("code", err.getCode());
                            o.put("message", err.getMessage());
                            self.notifyListeners("appOpenFailedToLoad", o);
                        }
                    });
                } catch (Throwable t) { loading = false; }
                call.resolve();
            }
        });
    }

    @PluginMethod
    public void isLoaded(PluginCall call) {
        JSObject o = new JSObject();
        o.put("loaded", ad != null && !showing);
        call.resolve(o);
    }

    @PluginMethod
    public void show(final PluginCall call) {
        if (ad == null || showing) {
            JSObject o = new JSObject(); o.put("shown", false); call.resolve(o); return;
        }
        final AppOpenPlugin self = this;
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                boolean ok = false;
                try {
                    ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdShowedFullScreenContent() {
                            showing = true;
                            self.notifyListeners("appOpenShowed", new JSObject());
                        }
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            ad = null; showing = false;
                            self.notifyListeners("appOpenDismissed", new JSObject());
                        }
                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError err) {
                            ad = null; showing = false;
                            self.notifyListeners("appOpenFailedToShow", new JSObject());
                        }
                    });
                    ad.show(getActivity());
                    ok = true;
                } catch (Throwable t) { showing = false; ad = null; }
                JSObject o = new JSObject(); o.put("shown", ok); call.resolve(o);
            }
        });
    }
}

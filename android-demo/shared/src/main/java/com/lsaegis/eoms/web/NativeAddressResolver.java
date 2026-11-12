package com.lsaegis.eoms.web;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Best-effort system address lookup. One provider call at a time; no UI-thread network work. */
final class NativeAddressResolver implements AutoCloseable {
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private boolean busy;
    private boolean closed;

    NativeAddressResolver(Context context) { this.context = context.getApplicationContext(); }

    void resolve(double latitude, double longitude, Consumer<String> callback) {
        if (closed || busy || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || Math.abs(latitude) > 90 || Math.abs(longitude) > 180 || !Geocoder.isPresent()) {
            callback.accept("");
            return;
        }
        busy = true;
        AtomicBoolean delivered = new AtomicBoolean();
        Runnable timeout = () -> { if (!closed && delivered.compareAndSet(false, true)) callback.accept(""); };
        main.postDelayed(timeout, 1800);
        Consumer<String> complete = address -> main.post(() -> {
            busy = false;
            main.removeCallbacks(timeout);
            if (!closed && delivered.compareAndSet(false, true)) callback.accept(address);
        });
        try {
            Geocoder geocoder = new Geocoder(context, Locale.SIMPLIFIED_CHINESE);
            if (Build.VERSION.SDK_INT >= 33) {
                geocoder.getFromLocation(latitude, longitude, 1, new Geocoder.GeocodeListener() {
                    @Override public void onGeocode(List<Address> addresses) { complete.accept(addressText(addresses)); }
                    @Override public void onError(String error) { complete.accept(""); }
                });
            } else {
                worker.execute(() -> {
                    try { complete.accept(addressText(geocoder.getFromLocation(latitude, longitude, 1))); }
                    catch (Exception failure) { complete.accept(""); }
                });
            }
        } catch (Exception failure) { complete.accept(""); }
    }

    private static String addressText(List<Address> addresses) {
        if (addresses == null || addresses.isEmpty()) return "";
        Address address = addresses.get(0);
        String text = address.getMaxAddressLineIndex() >= 0 ? address.getAddressLine(0) : null;
        if (text == null || text.trim().isEmpty()) return "";
        text = text.replaceAll("[\\r\\n\\t]+", " ").trim();
        return text.substring(0, Math.min(512, text.length()));
    }

    @Override public void close() {
        closed = true;
        main.removeCallbacksAndMessages(null);
        worker.shutdownNow();
    }
}

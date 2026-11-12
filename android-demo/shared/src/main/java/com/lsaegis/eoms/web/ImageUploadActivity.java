package com.lsaegis.eoms.web;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.webkit.PermissionRequest;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** Shared source chooser: delegated system camera and selection-scoped photo picker. */
public abstract class ImageUploadActivity extends Activity {
    private ValueCallback<Uri[]> pendingImages;
    private int selectionLimit = 1;
    private int requestSequence = 4100;
    private int activeRequest = -1;
    private boolean captureRequest;
    private boolean destroyed;
    private String selectionPage;
    private File captureFile;
    private Uri captureUri;
    private WebView webView;
    private Dialog sourceDialog;
    private static final int LOCATION_PERMISSION_REQUEST = 3001;
    private GeolocationPermissions.Callback locationCallback;
    private String locationOrigin;
    private String locationPage;
    private NativeAddressResolver addressResolver;
    private final ArrayList<File> retainedCaptures = new ArrayList<>();
    private final ExecutorService imageReader = Executors.newSingleThreadExecutor();
    protected abstract String entryUrl();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) WebView.setWebContentsDebuggingEnabled(true);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(false);
        try {
            String appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            settings.setUserAgentString(settings.getUserAgentString() + " LSAegisApp/" + appVersion);
        } catch (PackageManager.NameNotFoundException ignored) { }

        settings.setGeolocationEnabled(true);
        addressResolver = new NativeAddressResolver(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                if (trustedPage() && url != null && url.equals(view.getUrl()))
                    view.evaluateJavascript("window.__LS_AEGIS_NATIVE_GEOCODER__=true", null);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                finishSelection(null);
                if (!trustedPage()) { callback.onReceiveValue(null); return true; }
                pendingImages = callback;
                selectionPage = view.getUrl();
                activeRequest = ++requestSequence;
                final int request = activeRequest;
                final boolean multiple = params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE;
                view.evaluateJavascript("(()=>{const e=document.activeElement;const u=e&&e.closest('[data-photo-remaining]');return u?Number(u.dataset.photoRemaining):null})()", value -> {
                    if (!current(request)) return;
                    selectionLimit = ImageSelectionPolicy.selectionLimit(value, multiple);
                    if (selectionLimit == 0) { notifyUser("照片数量已达上限，请先删除不需要的照片"); finishSelection(null); return; }
                    showSources(request);
                });
                return true;
            }
            @Override public void onPermissionRequest(PermissionRequest request) {
                request.deny();
                if (ImageSelectionPolicy.sameOrigin(request.getOrigin().toString(), entryUrl())) notifyUser("请通过添加照片中的拍照入口使用系统相机");
            }
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                finishLocationPermission(false);
                if (!trustedPage() || !ImageSelectionPolicy.sameOrigin(origin, entryUrl())) {
                    callback.invoke(origin, false, false);
                    return;
                }
                locationCallback = callback;
                locationOrigin = origin;
                locationPage = webView.getUrl();
                if (hasLocationPermission()) finishLocationPermission(true);
                else requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            }
            @Override public void onGeolocationPermissionsHidePrompt() { finishLocationPermission(false); }
            @Override public boolean onJsPrompt(WebView view, String url, String message, String payload, JsPromptResult result) {
                if (!"LS_AEGIS_ADDRESS_V1".equals(message)) return super.onJsPrompt(view, url, message, payload, result);
                if (!trustedPage() || !hasLocationPermission() || url == null || !url.equals(view.getUrl())
                        || !ImageSelectionPolicy.sameOrigin(url, entryUrl()) || payload == null || payload.length() > 1024) {
                    result.confirm("unavailable");
                    return true;
                }
                boolean acknowledged = false;
                try {
                    JSONObject data = new JSONObject(payload);
                    String id = data.getString("id");
                    double latitude = data.getDouble("latitude"), longitude = data.getDouble("longitude");
                    if (!id.matches("[a-zA-Z0-9_-]{1,64}") || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                            || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) throw new IllegalArgumentException();
                    String page = view.getUrl();
                    result.confirm("accepted");
                    acknowledged = true;
                    addressResolver.resolve(latitude, longitude, address -> {
                        if (!trustedPage() || !page.equals(view.getUrl())) return;
                        String detail = "{id:" + JSONObject.quote(id) + ",address:" + JSONObject.quote(address) + "}";
                        view.evaluateJavascript("window.dispatchEvent(new CustomEvent('ls-aegis-address-result',{detail:" + detail + "}))", null);
                    });
                } catch (Exception failure) { if (!acknowledged) result.confirm("unavailable"); }
                return true;
            }
        });
        webView.setBackgroundColor(0xFFF4F6FB);
        setContentView(webView);
        if (state != null) {
            restorePendingCapture(state);
            requestSequence = Math.max(requestSequence, state.getInt("imageRequestSequence", requestSequence));
            if (state.getBoolean("pendingImageSelection")) notifyUser("系统已重建页面，请检查表单并重新添加照片");
        }
        webView.loadUrl(entryUrl());
    }
    private boolean trustedPage() { return !destroyed && !isFinishing() && !isDestroyed() && ImageSelectionPolicy.sameOrigin(webView.getUrl(), entryUrl()); }
    private boolean current(int request) { return pendingImages != null && activeRequest == request && trustedPage() && selectionPage.equals(webView.getUrl()); }
    private boolean hasLocationPermission() {
        return checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void finishLocationPermission(boolean allowed) {
        GeolocationPermissions.Callback callback = locationCallback;
        String origin = locationOrigin;
        boolean currentPage = callback != null && trustedPage() && locationPage != null && locationPage.equals(webView.getUrl());
        locationCallback = null; locationOrigin = null; locationPage = null;
        if (callback != null) callback.invoke(origin, allowed && currentPage, false);
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) finishLocationPermission(hasLocationPermission());
    }
    private void showSources(int request) {
        sourceDialog = new PhotoSourceDialog(this, selectionLimit,
            () -> selectSource(request, true), () -> selectSource(request, false),
            () -> finishSelection(null));
        sourceDialog.show();
    }
    private void selectSource(int request, boolean camera) {
        sourceDialog = null;
        if (!current(request)) { finishSelection(null); return; }
        if (camera) openCamera(request); else openGallery(request);
    }
    private void openGallery(int request) {
        captureRequest = false;
        Intent picker = null;
        if (Build.VERSION.SDK_INT >= 33) {
            picker = new Intent(MediaStore.ACTION_PICK_IMAGES).setType("image/*");
            if (selectionLimit > 1) picker.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, Math.min(selectionLimit, MediaStore.getPickImagesMaxLimit()));
            if (picker.resolveActivity(getPackageManager()) == null) picker = null;
        }
        if (picker == null) {
            picker = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
            picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png"});
            picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, selectionLimit > 1);
        }
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivityForResult(picker, request); }
        catch (ActivityNotFoundException | SecurityException ex) {
            finishSelection(null);
            notifyUser("相册或文件选择器无法打开，请检查系统应用后重试；表单内容已保留");
        }
    }
    private void openCamera(int request) {
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        var handler = camera.resolveActivity(getPackageManager());
        if (handler == null) { finishSelection(null); notifyUser("未找到可用相机，可以改为从相册选择"); return; }
        String cameraPackage = handler.getPackageName();
        try {
            File directory = new File(getCacheDir(), "upload-capture");
            if (!directory.isDirectory() && !directory.mkdirs()) throw new java.io.IOException("cache unavailable");
            captureFile = File.createTempFile("capture-", ".jpg", directory);
            captureUri = FileProvider.getUriForFile(this, getPackageName() + ".upload.files", captureFile);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, captureUri);
            camera.setClipData(ClipData.newRawUri("拍摄照片", captureUri));
            camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            captureRequest = true;
            startActivityForResult(camera, request);
        } catch (SecurityException ex) {
            finishSelection(null);
            new AlertDialog.Builder(this).setTitle("系统相机无法访问")
                .setMessage("可检查系统相机应用的权限，或返回后改用相册。当前表单内容不会清空。")
                .setPositiveButton("相机应用设置", (dialog, which) -> openCameraSettings(cameraPackage)).setNegativeButton("返回", null).show();
        } catch (java.io.IOException | IllegalArgumentException | ActivityNotFoundException ex) {
            finishSelection(null); notifyUser("无法创建照片或打开相机，请检查可用空间，也可以改用相册");
        }
    }
    private void openCameraSettings(String packageName) {
        try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName))); }
        catch (ActivityNotFoundException | SecurityException ex) { notifyUser("请在系统设置中检查相机应用权限"); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (pendingImages == null) {
            if (captureFile != null) { cleanupCapture(); notifyUser("页面已恢复，请重新添加照片"); }
            return;
        }
        if (request != activeRequest) return;
        if (result != RESULT_OK || !current(request)) { finishSelection(null); return; }
        if (!captureRequest && data != null && data.getClipData() != null && data.getClipData().getItemCount() > selectionLimit) {
            notifyUser("最多还能添加 " + selectionLimit + " 张照片，请重新选择；原有照片已保留");
            finishSelection(null);
            return;
        }
        LinkedHashSet<Uri> candidates = new LinkedHashSet<>();
        if (captureRequest) {
            if (captureUri != null) candidates.add(captureUri);
        } else if (data != null && data.getClipData() != null) {
            ClipData clips = data.getClipData();
            for (int i = 0; i < clips.getItemCount() && candidates.size() < selectionLimit; i++) candidates.add(clips.getItemAt(i).getUri());
        } else if (data != null && data.getData() != null) candidates.add(data.getData());
        imageReader.execute(() -> {
            ArrayList<Uri> valid = new ArrayList<>();
            for (Uri uri : candidates) if (readableImage(uri)) valid.add(uri);
            runOnUiThread(() -> {
                if (activeRequest != request) return;
                if (!current(request)) { finishSelection(null); return; }
                if (valid.size() != candidates.size() || valid.isEmpty()) notifyUser("部分照片不可读取或不是JPG/PNG，请重新选择或转换格式");
                if (captureRequest && !valid.isEmpty()) {
                    retainedCaptures.add(captureFile); revokeCaptureGrant(); captureFile = null; captureUri = null;
                }
                finishSelection(valid.isEmpty() ? null : valid.toArray(new Uri[0]));
            });
        });
    }
    private boolean readableImage(Uri uri) {
        if (uri == null || !"content".equals(uri.getScheme())) return false;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) return false;
            byte[] bytes = new byte[8]; int count = 0, n;
            while (count < bytes.length && (n = input.read(bytes, count, bytes.length - count)) > 0) count += n;
            return ImageSelectionPolicy.supportedHeader(java.util.Arrays.copyOf(bytes, count));
        } catch (java.io.IOException | SecurityException | IllegalArgumentException ex) { return false; }
    }
    private void finishSelection(Uri[] values) {
        ValueCallback<Uri[]> callback = pendingImages;
        pendingImages = null; activeRequest = -1; selectionPage = null;
        if (sourceDialog != null) { sourceDialog.dismiss(); sourceDialog = null; }
        cleanupCapture();
        if (callback != null) callback.onReceiveValue(values);
    }
    private void revokeCaptureGrant() {
        if (captureUri != null) revokeUriPermission(captureUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }
    private void cleanupCapture() {
        revokeCaptureGrant();
        if (captureFile != null) captureFile.delete();
        captureFile = null; captureUri = null; captureRequest = false;
    }
    private void notifyUser(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("pendingImageSelection", pendingImages != null);
        state.putInt("imageRequestSequence", requestSequence);
        if (captureFile != null) state.putString("pendingCapture", captureFile.getName());
        super.onSaveInstanceState(state);
    }
    private void restorePendingCapture(Bundle state) {
        String name = state.getString("pendingCapture");
        if (name != null && name.matches("capture-[a-zA-Z0-9-]+\\.jpg")) {
            captureFile = new File(new File(getCacheDir(), "upload-capture"), name);
            captureUri = FileProvider.getUriForFile(this, getPackageName() + ".upload.files", captureFile);
        }
    }
    @Override protected void onDestroy() {
        destroyed = true;
        if (addressResolver != null) addressResolver.close();
        finishLocationPermission(false);
        if (isChangingConfigurations()) { captureFile = null; captureUri = null; }
        finishSelection(null);
        imageReader.shutdownNow();
        for (File file : retainedCaptures) file.delete();
        if (webView != null) { webView.stopLoading(); webView.setWebChromeClient(null); webView.destroy(); }
        super.onDestroy();
    }
}

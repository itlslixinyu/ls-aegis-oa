package com.lsaegis.eoms.web;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** App-owned source menu; the camera and photo picker remain system activities. */
final class PhotoSourceDialog extends Dialog {
    private static final int INK = Color.rgb(30, 45, 67);
    private static final int MUTED = Color.rgb(112, 126, 146);
    private static final int BLUE = Color.rgb(61, 110, 211);

    PhotoSourceDialog(Activity activity, int remaining, Runnable camera, Runnable gallery, Runnable cancel) {
        super(activity);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCanceledOnTouchOutside(true);
        setOnCancelListener(dialog -> cancel.run());

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), dp(16));
        GradientDrawable surface = shape(Color.WHITE, 0);
        surface.setCornerRadii(new float[]{dp(24), dp(24), dp(24), dp(24), 0, 0, 0, 0});
        content.setBackground(surface);

        View handle = new View(activity);
        handle.setBackground(shape(Color.rgb(219, 226, 236), 4));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(32), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(18);
        content.addView(handle, handleParams);

        TextView title = text("添加照片", 19, INK);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        content.addView(title);
        TextView subtitle = text("仅访问您选择的照片 · 还可添加 " + remaining + " 张", 12, MUTED);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(7), 0, dp(20));
        content.addView(subtitle);

        LinearLayout choices = new LinearLayout(activity);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        choices.setBaselineAligned(false);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -1, 1);
        left.setMarginEnd(dp(6));
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, -1, 1);
        right.setMarginStart(dp(6));
        choices.addView(option(activity, true, "拍照", "拍摄现场照片", camera), left);
        choices.addView(option(activity, false, "从相册选择", "选择已有照片", gallery), right);
        content.addView(choices, new LinearLayout.LayoutParams(-1, -2));

        TextView cancelButton = text("取消", 15, MUTED);
        cancelButton.setGravity(Gravity.CENTER);
        cancelButton.setMinHeight(dp(48));
        cancelButton.setPadding(dp(12), dp(12), dp(12), dp(12));
        cancelButton.setBackground(ripple(Color.rgb(245, 247, 251)));
        cancelButton.setOnClickListener(view -> cancel());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, -2);
        cancelParams.topMargin = dp(16);
        content.addView(cancelButton, cancelParams);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.addView(content);
        setContentView(scroll);
    }

    private View option(Activity activity, boolean cameraIcon, String title, String description, Runnable action) {
        LinearLayout tile = new LinearLayout(activity);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setMinimumHeight(dp(130));
        tile.setPadding(dp(8), dp(16), dp(8), dp(16));
        tile.setBackground(ripple(Color.rgb(244, 247, 253)));
        tile.setFocusable(true);
        tile.setContentDescription(title + "，" + description);
        tile.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        View icon = new SourceIcon(activity, cameraIcon);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView label = text(title, 15, INK);
        label.setGravity(Gravity.CENTER);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setPadding(0, dp(10), 0, dp(5));
        tile.addView(label);
        TextView hint = text(description, 11, MUTED);
        hint.setGravity(Gravity.CENTER);
        tile.addView(hint);
        tile.setOnClickListener(view -> { dismiss(); action.run(); });
        return tile;
    }

    @Override public void show() {
        super.show();
        Window window = getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        attrs.width = Math.min(getContext().getResources().getDisplayMetrics().widthPixels, dp(560));
        attrs.height = WindowManager.LayoutParams.WRAP_CONTENT;
        attrs.dimAmount = 0.35f;
        window.setAttributes(attrs);
        window.setNavigationBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 26) window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private TextView text(String value, int size, int color) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        return text;
    }
    private int dp(float value) { return Math.round(value * getContext().getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }
    private RippleDrawable ripple(int color) {
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(30, 61, 110, 211)), shape(color, 14), shape(Color.WHITE, 14));
    }

    private static final class SourceIcon extends View {
        private final boolean camera;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        SourceIcon(Activity activity, boolean camera) { super(activity); this.camera = camera; }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();
            canvas.scale(getWidth() / 36f, getHeight() / 36f);
            paint.setColor(BLUE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.8f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            if (camera) {
                android.graphics.Path outline = new android.graphics.Path();
                outline.moveTo(6, 10); outline.lineTo(12, 10); outline.lineTo(14, 6);
                outline.lineTo(22, 6); outline.lineTo(24, 10); outline.lineTo(30, 10);
                outline.quadTo(32, 10, 32, 12); outline.lineTo(32, 28);
                outline.quadTo(32, 30, 30, 30); outline.lineTo(6, 30);
                outline.quadTo(4, 30, 4, 28); outline.lineTo(4, 12);
                outline.quadTo(4, 10, 6, 10); canvas.drawPath(outline, paint);
                canvas.drawCircle(18, 20, 6, paint);
            } else {
                canvas.drawRoundRect(5, 5, 31, 31, 4, 4, paint);
                canvas.drawCircle(13, 13, 2.5f, paint);
                android.graphics.Path mountain = new android.graphics.Path();
                mountain.moveTo(6, 26); mountain.lineTo(14, 19); mountain.lineTo(19, 23);
                mountain.lineTo(25, 16); mountain.lineTo(31, 22); canvas.drawPath(mountain, paint);
            }
            canvas.restore();
        }
    }
}

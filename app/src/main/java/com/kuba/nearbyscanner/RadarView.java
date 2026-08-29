package com.kuba.nearbyscanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class RadarView extends View {
    static final int MODE_ALL = 0;
    static final int MODE_BLUETOOTH = 1;
    static final int MODE_WIFI = 2;
    static final int MODE_HIDDEN = 3;

    static final class RadarDevice {
        final String id;
        final String name;
        final String transport;
        final String type;
        final double distance;
        final int rssi;
        final boolean hidden;

        RadarDevice(String id, String name, String transport, String type,
                    double distance, int rssi, boolean hidden) {
            this.id = id;
            this.name = name;
            this.transport = transport;
            this.type = type;
            this.distance = distance;
            this.rssi = rssi;
            this.hidden = hidden;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<RadarDevice> devices = new ArrayList<>();
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private int mode = MODE_ALL;
    private float zoom = 1f;
    private float panX;
    private float panY;

    RadarView(Context context) {
        super(context);
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                zoom = Math.max(.65f, Math.min(4.5f, zoom * detector.getScaleFactor()));
                invalidate();
                return true;
            }
        });
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent event) { return true; }
            @Override public boolean onScroll(MotionEvent first, MotionEvent current, float dx, float dy) {
                if (!scaleDetector.isInProgress()) {
                    panX = clamp(panX - dx, -getWidth(), getWidth());
                    panY = clamp(panY - dy, -getHeight(), getHeight());
                    invalidate();
                }
                return true;
            }
            @Override public boolean onDoubleTap(MotionEvent event) {
                resetView();
                return true;
            }
        });
    }

    void setDevices(List<RadarDevice> values) {
        devices.clear();
        if (values != null) devices.addAll(values);
        invalidate();
    }

    void setMode(int value) {
        mode = value;
        invalidate();
    }

    int getMode() { return mode; }

    void copyDevicesFrom(RadarView source) {
        setDevices(source == null ? null : source.devices);
        if (source != null) setMode(source.mode);
    }

    void resetView() {
        zoom = 1f;
        panX = 0f;
        panY = 0f;
        invalidate();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        boolean scaled = scaleDetector.onTouchEvent(event);
        boolean gestured = gestureDetector.onTouchEvent(event);
        return scaled || gestured || super.onTouchEvent(event);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        float cx = w / 2f;
        float cy = h * 0.52f;
        float radius = Math.min(w * 0.44f, h * 0.39f);

        canvas.drawColor(Color.rgb(5, 14, 22));
        canvas.save();
        canvas.translate(panX, panY);
        canvas.scale(zoom, zoom, cx, cy);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new android.graphics.RadialGradient(cx, cy, radius * 1.15f,
                new int[]{Color.rgb(15, 63, 67), Color.rgb(7, 26, 36), Color.rgb(5, 14, 22)},
                new float[]{0f, .55f, 1f}, android.graphics.Shader.TileMode.CLAMP));
        canvas.drawOval(cx - radius, cy - radius * .58f, cx + radius, cy + radius * .58f, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        for (int ring = 1; ring <= 4; ring++) {
            float r = radius * ring / 4f;
            paint.setColor(Color.argb(ring == 4 ? 130 : 70, 73, 214, 178));
            canvas.drawOval(cx - r, cy - r * .58f, cx + r, cy + r * .58f, paint);
        }
        for (int angle = 0; angle < 360; angle += 30) {
            double a = Math.toRadians(angle);
            paint.setColor(Color.argb(45, 73, 214, 178));
            canvas.drawLine(cx, cy, cx + (float) Math.cos(a) * radius,
                    cy + (float) Math.sin(a) * radius * .58f, paint);
        }

        double sweep = (SystemClock.uptimeMillis() % 5000L) / 5000.0 * Math.PI * 2.0;
        Path beam = new Path();
        beam.moveTo(cx, cy);
        beam.lineTo(cx + (float) Math.cos(sweep - .18) * radius,
                cy + (float) Math.sin(sweep - .18) * radius * .58f);
        beam.lineTo(cx + (float) Math.cos(sweep) * radius,
                cy + (float) Math.sin(sweep) * radius * .58f);
        beam.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(38, 73, 214, 178));
        canvas.drawPath(beam, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(Color.argb(210, 73, 214, 178));
        canvas.drawLine(cx, cy, cx + (float) Math.cos(sweep) * radius,
                cy + (float) Math.sin(sweep) * radius * .58f, paint);

        int visible = 0;
        for (RadarDevice device : devices) {
            if (!visible(device)) continue;
            visible++;
            drawMarker(canvas, device, cx, cy, radius);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, dp(7), paint);
        paint.setColor(Color.rgb(73, 214, 178));
        canvas.drawCircle(cx, cy, dp(3), paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(10));
        paint.setColor(Color.rgb(190, 210, 220));
        canvas.drawText("MŮJ TELEFON", cx, cy + dp(24), paint);
        canvas.restore();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(dp(11));
        paint.setColor(Color.rgb(157, 177, 190));
        canvas.drawText(visible + " zařízení • zoom " + String.format(Locale.ROOT, "%.1f×", zoom)
                + " • táhni / přibliž dvěma prsty / dvojklik reset", dp(12), h - dp(12), paint);
        postInvalidateDelayed(50);
    }

    private boolean visible(RadarDevice d) {
        if (mode == MODE_BLUETOOTH) return "BLUETOOTH".equals(d.transport);
        if (mode == MODE_WIFI) return "WIFI".equals(d.transport);
        if (mode == MODE_HIDDEN) return d.hidden;
        return "BLUETOOTH".equals(d.transport) || "WIFI".equals(d.transport);
    }

    private void drawMarker(Canvas canvas, RadarDevice d, float cx, float cy, float radius) {
        int hash = d.id == null ? 1 : d.id.hashCode();
        double angle = ((hash & 0x7fffffff) % 360) * Math.PI / 180.0;
        double meters = d.distance >= 0 ? d.distance : Math.max(.5, Math.pow(10, (-50 - d.rssi) / 25.0));
        float scale = (float) Math.min(.94, .12 + Math.log10(1 + meters) / 2.15);
        float x = cx + (float) Math.cos(angle) * radius * scale;
        float baseY = cy + (float) Math.sin(angle) * radius * scale * .58f;
        float elevation = ((Math.abs(hash / 31) % 6) + 1) * dp(4);
        float y = baseY - elevation;
        int color = markerColor(d);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.argb(100, Color.red(color), Color.green(color), Color.blue(color)));
        canvas.drawLine(x, baseY, x, y, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(dp(9), 0, 0, color);
        paint.setColor(color);
        canvas.drawCircle(x, y, dp(d.hidden ? 7 : 6), paint);
        paint.clearShadowLayer();
        paint.setColor(Color.rgb(5, 14, 22));
        canvas.drawCircle(x, y, dp(2), paint);

        String name = d.hidden ? "SKRYTÉ • " + transportLabel(d.transport) : shorten(d.name, 17);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(9));
        paint.setColor(Color.WHITE);
        canvas.drawText(name, x, y - dp(11), paint);
        paint.setTextSize(dp(8));
        paint.setColor(Color.rgb(170, 194, 205));
        String range = d.distance < 0 ? d.rssi + " dBm" : String.format(Locale.forLanguageTag("cs-CZ"), "≈ %.1f m", d.distance);
        canvas.drawText(range, x, y + dp(18), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private int markerColor(RadarDevice d) {
        if (d.hidden) return Color.rgb(255, 184, 77);
        if ("TRACKER".equals(d.type)) return Color.rgb(255, 184, 77);
        if ("CAMERA".equals(d.type)) return Color.rgb(255, 103, 116);
        if ("AUDIO".equals(d.type)) return Color.rgb(151, 123, 255);
        return "WIFI".equals(d.transport) ? Color.rgb(77, 166, 255) : Color.rgb(78, 205, 255);
    }

    private String transportLabel(String value) {
        return "WIFI".equals(value) ? "Wi‑Fi" : "Bluetooth";
    }

    private String shorten(String value, int max) {
        if (value == null || value.isEmpty()) return "Neznámé";
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

package opentok.bitmap.sample;

import android.content.Context;
import android.util.Log;
import com.opentok.android.BaseVideoCapturer;

public class CustomVideoCapturer extends BaseVideoCapturer
{

    private final static String LOGTAG = "customer-video-capturer";

    private boolean isCaptureStarted = false;
    private boolean isCaptureRunning = false;

    private int mCaptureWidth = 384;
    private int mCaptureHeight = 288;
    private int mCaptureFPS = -1;

    public CustomVideoCapturer(Context context) {}

    @Override
    public int startCapture() {
        if (isCaptureStarted) {
            return -1;
        }

        isCaptureRunning = true;
        isCaptureStarted = true;

        return 0;
    }

    @Override
    public int stopCapture() {
   
        try {
            if (isCaptureRunning) {
                isCaptureRunning = false;
            }
        } catch (RuntimeException e) {
            Log.e(LOGTAG, "Failed to stop camera", e);
            return -1;
        }

        isCaptureStarted = false;
        return 0;
    }

    @Override
    public void destroy() {
        stopCapture();
    }

    @Override
    public boolean isCaptureStarted() {
        return isCaptureStarted;
    }

    @Override
    public CaptureSettings getCaptureSettings() {

        CaptureSettings settings = new CaptureSettings();
        settings.fps = mCaptureFPS;
        settings.width = mCaptureWidth;
        settings.height = mCaptureHeight;
        settings.format = ARGB;
        settings.expectedDelay = 0;
        return settings;
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void init() {

    }



}

package com.koushikdutta.virtualdisplay;

import android.graphics.Point;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.xing.xbase.util.LogUtil;

import org.xml.sax.Attributes;

import java.io.IOException;

public abstract class EncoderDevice {
    protected final String LOGTAG;
    int colorFormat;
    Point encSize;
    protected int mHeight;
    protected Thread lastRecorderThread;
    public String name;
    boolean useEncodingConstraints;
    boolean useSurface;
    protected VirtualDisplayFactory vdf;
    protected MediaCodec mMediaCodec;
    protected VirtualDisplay virtualDisplay;
    protected int mWidth;

    public EncoderDevice(final String name, final int mWidth, final int mHeight) {
        LOGTAG = getClass().getSimpleName();
        useSurface = true;
        useEncodingConstraints = true;
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.name = name;
        LogUtil.d("Requested Width: " + mWidth + " Requested Height: " + mHeight);
    }

    public static int getSupportedDimension(final int n) {
        int n2 = 144;
        if (n > n2) {
            if (n <= 176) {
                n2 = 176;
            } else if (n <= 240) {
                n2 = 240;
            } else if (n <= 288) {
                n2 = 288;
            } else if (n <= 320) {
                n2 = 320;
            } else if (n <= 352) {
                n2 = 352;
            } else if (n <= 480) {
                n2 = 480;
            } else if (n <= 576) {
                n2 = 576;
            } else if (n <= 720) {
                n2 = 720;
            } else if (n <= 1024) {
                n2 = 1024;
            } else if (n <= 1280) {
                n2 = 1280;
            } else {
                n2 = 1920;
            }
        }
        return n2;
    }

    private static boolean isRecognizedFormat(final int n) {
        boolean b = false;
        switch (n) {
            default: {
                b = false;
                break;
            }
            case 19:
            case 20:
            case 21:
            case 39:
            case 2130706688: {
                b = true;
                break;
            }
        }
        return b;
    }

    private int selectColorFormat(final MediaCodecInfo mediaCodecInfo, final String s) throws Exception {
        final MediaCodecInfo.CodecCapabilities capabilitiesForType = mediaCodecInfo.getCapabilitiesForType(s);
        LogUtil.d("Available color formats: " + capabilitiesForType.colorFormats.length);
        for (int i = 0; i < capabilitiesForType.colorFormats.length; ++i) {
            final int n = capabilitiesForType.colorFormats[i];
            if (isRecognizedFormat(n)) {
                LogUtil.d("Using: " + n);
                return n;
            }
            LogUtil.d("Not using: " + n);
        }
        throw new Exception("Unable to find suitable color format");
    }

    private MediaCodecInfo findEncoder() {
        MediaCodecInfo codecInfo = null;
        try {
            int numCodecs = MediaCodecList.getCodecCount();
            for (int i = 0; i < numCodecs; i++) {
                MediaCodecInfo found = MediaCodecList.getCodecInfoAt(i);

                if (!found.isEncoder())
                    continue;

                String types[] = found.getSupportedTypes();
                for (int j = 0; j < types.length; j++) {
                    String type = types[j];
                    if (!type.equalsIgnoreCase("video/avc"))
                        continue;

                    if (codecInfo == null)
                        codecInfo = found;

                    MediaCodecInfo.CodecCapabilities caps = codecInfo.getCapabilitiesForType("video/avc");
                    int attr[] = caps.colorFormats;

                    for (int k = 0; k < attr.length; k++) {
                        Log.i(LOGTAG, "colorFormat: " + attr[k]);
                    }

                    MediaCodecInfo.CodecProfileLevel level[] = caps.profileLevels;

                    for (int k = 0; k < level.length; k++) {
                        Log.i(LOGTAG, "profile/level: " + level[k].profile + "/" + level[k].level);
                    }

                }
            }

        } catch (Exception e) {
            Log.w(LOGTAG, "Failed to create MeidaCodec " + e.getMessage());
            return null;
        }

        return codecInfo;
    }


    public Surface createDisplaySurface() {
        if (android.os.Build.VERSION.SDK_INT < 18)
            return null;

        signalEnd();
        mMediaCodec = null;

        if (findEncoder() == null) {
            return null;
        }
        // 根据视频质量计算相关参数
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 3000 * 1000);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        try {
            mMediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        Surface localSurface = mMediaCodec.createInputSurface();
        mMediaCodec.start();

        EncoderRunnable encoderRunnable = onSurfaceCreated(mMediaCodec);
        Thread encoderThread = new Thread(encoderRunnable, "Encoder");
        encoderThread.start();
        return localSurface;
    }

    Surface createInputSurface() {
        return mMediaCodec.createInputSurface();
    }

    void destroyDisplaySurface(final MediaCodec mediaCodec) {
        if (mediaCodec != null) {
            while (true) {
                try {
                    mediaCodec.stop();
                    mediaCodec.release();
                    if (mMediaCodec == mediaCodec) {
                        mMediaCodec = null;
                        if (virtualDisplay != null) {
                            virtualDisplay.release();
                            virtualDisplay = null;
                        }
                        if (vdf != null) {
                            vdf.release();
                            vdf = null;
                        }
                    }
                } catch (Exception ex) {
                    continue;
                }
                break;
            }
        }
    }

    public int getBitrate(final int n) {
        return 2000000;
    }

    public int getColorFormat() {
        return colorFormat;
    }

    public Point getEncodingDimensions() {
        return encSize;
    }

    public MediaCodec getMediaCodec() {
        return mMediaCodec;
    }

    public boolean isConnected() {
        return mMediaCodec != null;
    }

    public void joinRecorderThread() {
        try {
            if (lastRecorderThread != null) {
                lastRecorderThread.join();
            }
        } catch (InterruptedException ex) {
        }
    }

    protected abstract EncoderRunnable onSurfaceCreated(final MediaCodec p0);

    void setSurfaceFormat(final MediaFormat mediaFormat) {
        mediaFormat.setInteger("color-format", colorFormat = 2130708361);
    }

    public void registerVirtualDisplay(final VirtualDisplayFactory vdf, final int n) {
        assert virtualDisplay == null;
        Surface displaySurface = createDisplaySurface();
        if (displaySurface == null) {
            System.out.println("Unable to create surface" + "\n");
        } else {
            System.out.println("Created surface" + "\n");
            this.vdf = vdf;
            virtualDisplay = vdf.createVirtualDisplay(name, mWidth, mHeight, n, 3, displaySurface, null);
        }
    }

    public void setUseEncodingConstraints(final boolean useEncodingConstraints) {
        this.useEncodingConstraints = useEncodingConstraints;
    }

    void signalEnd() {
        if (mMediaCodec == null) {
            return;
        }
        try {
            mMediaCodec.signalEndOfInputStream();
        } catch (Exception ex) {
        }
    }

    public void stop() {
        if (Build.VERSION.SDK_INT >= 18) {
            signalEnd();
        }
        mMediaCodec = null;
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (vdf != null) {
            vdf.release();
            vdf = null;
        }
    }

    public boolean supportsSurface() {
        return Build.VERSION.SDK_INT >= 19 && useSurface;
    }

    public void useSurface(final boolean useSurface) {
        this.useSurface = useSurface;
    }

    protected abstract class EncoderRunnable implements Runnable {
        MediaCodec mEncoder;

        public EncoderRunnable(final MediaCodec mEncoder) {
            this.mEncoder = mEncoder;
        }

        protected void cleanup() {
            destroyDisplaySurface(mEncoder);
            mEncoder = null;
        }

        protected abstract void encode() throws Exception;

        @Override
        public final void run() {
            while (true) {
                try {
                    encode();
                    cleanup();
                } catch (Exception ex) {
                    continue;
                }
                break;
            }
        }
    }

    private static class VideoEncoderCap {
        int maxBitRate;
        int maxFrameHeight;
        int maxFrameRate;
        int maxFrameWidth;

        public VideoEncoderCap(final Attributes attributes) {
            maxFrameWidth = Integer.valueOf(attributes.getValue("maxFrameWidth"));
            maxFrameHeight = Integer.valueOf(attributes.getValue("maxFrameHeight"));
            maxBitRate = Integer.valueOf(attributes.getValue("maxBitRate"));
            maxFrameRate = Integer.valueOf(attributes.getValue("maxFrameRate"));
        }
    }
}

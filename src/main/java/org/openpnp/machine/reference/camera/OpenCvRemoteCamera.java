/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.camera;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.Videoio;
import org.opencv.videoio.VideoCapture;
import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.OpenCvRemoteCameraConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.logging.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.line;

/**
 * A Camera implementation based on the OpenCV FrameGrabbers.
 */
public class OpenCvRemoteCamera extends ReferenceCamera implements Runnable {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    @Attribute(name = "deviceIndex", required = true)
    private String deviceURI = "";

    @Attribute(required = false)
    private int preferredWidth;
    @Attribute(required = false)
    private int preferredHeight;
    @Attribute(required = false)
    private int fps = 24;

    @ElementList(required=false)
    private List<OpenCvCapturePropertyValue> properties = new ArrayList<>();

    private VideoCapture fg = new VideoCapture();
    private Thread thread;
    private boolean dirty = false;

    public OpenCvRemoteCamera() {}

    @Override
    public synchronized BufferedImage internalCapture() {
        if (thread == null) {
            initCamera();
        }
        Mat mat = new Mat();
        try {
            if (!fg.read(mat)) {
                Imgproc.line(mat, new Point(0, 0), new Point(preferredWidth, preferredHeight), new Scalar(0, 255, 0, 255));
                Imgproc.line(mat, new Point(0, preferredHeight), new Point(preferredWidth, 0), new Scalar(0, 255, 0, 255));
            }
            BufferedImage img = OpenCvUtils.toBufferedImage(mat);
            return transformImage(img);
        }
        catch (Exception e) {
            return null;
        }
        finally {
            mat.release();
        }
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener, int maximumFps) {
        if (thread == null) {
            initCamera();
        }
        super.startContinuousCapture(listener, maximumFps);
    }

    public void run() {
        while (!Thread.interrupted()) {
            try {
                BufferedImage image = internalCapture();
                if (image != null) {
                    broadcastCapture(image);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000 / fps);
            }
            catch (InterruptedException e) {
                break;
            }
        }
    }

    private void initCamera() {
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            thread = null;
        }
        try {
            setDirty(false);
            width = null;
            height = null;
            
            fg.open(deviceURI);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            }
            catch (Exception e) {

            }
        }
        if (fg.isOpened()) {
            fg.release();
        }
    }
    
    public double getOpenCvCapturePropertyValue(OpenCvCaptureProperty property) {
        return fg.get(property.openCvPropertyId);
    }

    public String getDeviceURI() {
        return deviceURI;
    }

    public synchronized void setDeviceURI(String deviceURI) {
        this.deviceURI = deviceURI;

        initCamera();
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = preferredWidth;
        setDirty(true);
    }

    public int getPreferredHeight() {
        return preferredHeight;
    }

    public void setPreferredHeight(int preferredHeight) {
        this.preferredHeight = preferredHeight;
        setDirty(true);
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new OpenCvRemoteCameraConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public List<OpenCvCapturePropertyValue> getProperties() {
        return properties;
    }

    public enum OpenCvCaptureProperty {
        CAP_PROP_POS_MSEC(0), // !< Current position of the video file in milliseconds.
        CAP_PROP_POS_FRAMES(1), // !< 0-based index of the frame to be decoded/captured next.
        CAP_PROP_POS_AVI_RATIO(2), // !< Relative position of the video file: 0=start of the film,
                                   // 1=end of the film.
        CAP_PROP_FRAME_WIDTH(3), // !< Width of the frames in the video stream.
        CAP_PROP_FRAME_HEIGHT(4), // !< Height of the frames in the video stream.
        CAP_PROP_FPS(5), // !< Frame rate.
        CAP_PROP_FOURCC(6), // !< 4-character code of codec. see VideoWriter::fourcc .
        CAP_PROP_FRAME_COUNT(7), // !< Number of frames in the video file.
        CAP_PROP_FORMAT(8), // !< Format of the %Mat objects returned by VideoCapture::retrieve().
        CAP_PROP_MODE(9), // !< Backend-specific value indicating the current capture mode.
        CAP_PROP_BRIGHTNESS(10), // !< Brightness of the image (only for cameras).
        CAP_PROP_CONTRAST(11), // !< Contrast of the image (only for cameras).
        CAP_PROP_SATURATION(12), // !< Saturation of the image (only for cameras).
        CAP_PROP_HUE(13), // !< Hue of the image (only for cameras).
        CAP_PROP_GAIN(14), // !< Gain of the image (only for cameras).
        CAP_PROP_EXPOSURE(15), // !< Exposure (only for cameras).
        CAP_PROP_CONVERT_RGB(16), // !< Boolean flags indicating whether images should be converted
                                  // to RGB.
        CAP_PROP_WHITE_BALANCE_BLUE_U(17), // !< Currently unsupported.
        CAP_PROP_RECTIFICATION(18), // !< Rectification flag for stereo cameras (note: only
                                    // supported by DC1394 v 2.x backend currently).
        CAP_PROP_MONOCHROME(19),
        CAP_PROP_SHARPNESS(20),
        CAP_PROP_AUTO_EXPOSURE(21), // !< DC1394: exposure control done by camera, user can adjust
                                    // reference level using this feature.
        CAP_PROP_GAMMA(22),
        CAP_PROP_TEMPERATURE(23),
        CAP_PROP_TRIGGER(24),
        CAP_PROP_TRIGGER_DELAY(25),
        CAP_PROP_WHITE_BALANCE_RED_V(26),
        CAP_PROP_ZOOM(27),
        CAP_PROP_FOCUS(28),
        CAP_PROP_GUID(29),
        CAP_PROP_ISO_SPEED(30),
        CAP_PROP_BACKLIGHT(32),
        CAP_PROP_PAN(33),
        CAP_PROP_TILT(34),
        CAP_PROP_ROLL(35),
        CAP_PROP_IRIS(36),
        CAP_PROP_SETTINGS(37), // ! Pop up video/camera filter dialog (note: only supported by DSHOW
                               // backend currently. Property value is ignored)
        CAP_PROP_BUFFERSIZE(38),
        CAP_PROP_AUTOFOCUS(39);
        
        private final int openCvPropertyId;

        private OpenCvCaptureProperty(int openCvPropertyId) {
            this.openCvPropertyId = openCvPropertyId;
        }

        public int getPropertyId() {
            return openCvPropertyId;
        }
    }

    public static class OpenCvCapturePropertyValue {
        @Attribute
        public OpenCvCaptureProperty property;
        @Attribute
        public double value;
        @Attribute
        public boolean setBeforeOpen;
        @Attribute
        public boolean setAfterOpen;
    }
}

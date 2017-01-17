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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.OpenCvCameraConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.OpenCvUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

/**
 * A Camera implementation based on the OpenCV FrameGrabbers.
 */
public class OpenCvCamera extends ReferenceCamera implements Runnable {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    @Attribute(name = "deviceIndex", required = true)
    private int deviceIndex = 0;

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

    public OpenCvCamera() {}

    @Override
    public synchronized BufferedImage internalCapture() {
        if (thread == null) {
            initCamera();
        }
        Mat mat = new Mat();
        try {
            if (!fg.read(mat)) {
                return null;
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

            for (OpenCvCapturePropertyValue pv : properties) {
                if (pv.setBeforeOpen) {
                    Logger.debug("Setting property {} on camera {} to {}", pv.property.toString(), this,pv.value);
                    fg.set(pv.property.getPropertyId(), pv.value);
                }
            }
            /**
             * Based on comments in https://github.com/openpnp/openpnp/issues/395 some cameras
             * may only handle resolution changes before opening while others handle it after
             * so we do both to try to cover both cases.
             */
            if (preferredWidth != 0) {
                Logger.debug("Setting camera {} width to {}", this, preferredWidth);
                fg.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, preferredWidth);
                Logger.debug("Camera {} reports width {}", this, fg.get(Videoio.CV_CAP_PROP_FRAME_WIDTH));
            }
            if (preferredHeight != 0) {
                Logger.debug("Setting camera {} height to {}", this, preferredHeight);
                fg.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, preferredHeight);
                Logger.debug("Camera {} reports height {}", this, fg.get(Videoio.CV_CAP_PROP_FRAME_HEIGHT));
            }
            
            fg.open(deviceIndex);
            
            for (OpenCvCaptureProperty property : OpenCvCaptureProperty.values()) {
                Logger.trace("{} {} = {}", this, property, getOpenCvCapturePropertyValue(property));
            }
            
            for (OpenCvCapturePropertyValue pv : properties) {
                if (pv.setAfterOpen) {
                    Logger.debug("Setting property {} on camera {} to {}", pv.property.toString(), this, pv.value);
                    fg.set(pv.property.getPropertyId(), pv.value);
                }
            }
            /**
             * Based on comments in https://github.com/openpnp/openpnp/issues/395 some cameras
             * may only handle resolution changes before opening while others handle it after
             * so we do both to try to cover both cases.
             */
            if (preferredWidth != 0) {
                Logger.debug("Setting camera {} width to {}", this, preferredWidth);
                fg.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, preferredWidth);
                Logger.debug("Camera {} reports width {}", this, fg.get(Videoio.CV_CAP_PROP_FRAME_WIDTH));
            }
            if (preferredHeight != 0) {
                Logger.debug("Setting camera {} height to {}", this, preferredHeight);
                fg.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, preferredHeight);
                Logger.debug("Camera {} reports height {}", this, fg.get(Videoio.CV_CAP_PROP_FRAME_HEIGHT));
            }
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

    public int getDeviceIndex() {
        return deviceIndex;
    }

    public synchronized void setDeviceIndex(int deviceIndex) {
        this.deviceIndex = deviceIndex;

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
        return new OpenCvCameraConfigurationWizard(this);
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
        CAP_PROP_POS_MSEC(Videoio.CAP_PROP_POS_MSEC), // !< Current position of the video file in milliseconds.
        CAP_PROP_POS_FRAMES(Videoio.CAP_PROP_POS_FRAMES), // !< 0-based index of the frame to be decoded/captured next.
        CAP_PROP_POS_AVI_RATIO(Videoio.CAP_PROP_POS_AVI_RATIO), // !< Relative position of the video file: 0=start of the film,
                                   // 1=end of the film.
        CAP_PROP_FRAME_WIDTH(Videoio.CAP_PROP_FRAME_WIDTH), // !< Width of the frames in the video stream.
        CAP_PROP_FRAME_HEIGHT(Videoio.CAP_PROP_FRAME_HEIGHT), // !< Height of the frames in the video stream.
        CAP_PROP_FPS(Videoio.CAP_PROP_FPS), // !< Frame rate.
        CAP_PROP_FOURCC(Videoio.CAP_PROP_FOURCC), // !< 4-character code of codec. see VideoWriter::fourcc .
        CAP_PROP_FRAME_COUNT(Videoio.CAP_PROP_FRAME_COUNT), // !< Number of frames in the video file.
        CAP_PROP_FORMAT(Videoio.CAP_PROP_FORMAT), // !< Format of the %Mat objects returned by VideoCapture::retrieve(Videoio.retrieve).
        CAP_PROP_MODE(Videoio.CAP_PROP_MODE), // !< Backend-specific value indicating the current capture mode.
        CAP_PROP_BRIGHTNESS(Videoio.CAP_PROP_BRIGHTNESS), // !< Brightness of the image (only for cameras).
        CAP_PROP_CONTRAST(Videoio.CAP_PROP_CONTRAST), // !< Contrast of the image (only for cameras).
        CAP_PROP_SATURATION(Videoio.CAP_PROP_SATURATION), // !< Saturation of the image (only for cameras).
        CAP_PROP_HUE(Videoio.CAP_PROP_HUE), // !< Hue of the image (only for cameras).
        CAP_PROP_GAIN(Videoio.CAP_PROP_GAIN), // !< Gain of the image (only for cameras).
        CAP_PROP_EXPOSURE(Videoio.CAP_PROP_EXPOSURE), // !< Exposure (only for cameras).
        CAP_PROP_CONVERT_RGB(Videoio.CAP_PROP_CONVERT_RGB), // !< Boolean flags indicating whether images should be converted
                                  // to RGB.
        CAP_PROP_WHITE_BALANCE_BLUE_U(Videoio.CAP_PROP_WHITE_BALANCE_BLUE_U), // !< Currently unsupported.
        CAP_PROP_RECTIFICATION(Videoio.CAP_PROP_RECTIFICATION), // !< Rectification flag for stereo cameras (note: only
                                    // supported by DC1394 v 2.x backend currently).
        CAP_PROP_MONOCHROME(Videoio.CAP_PROP_MONOCHROME),
        CAP_PROP_SHARPNESS(Videoio.CAP_PROP_SHARPNESS),
        CAP_PROP_AUTO_EXPOSURE(Videoio.CAP_PROP_AUTO_EXPOSURE), // !< DC1394: exposure control done by camera, user can adjust
                                    // reference level using this feature.
        CAP_PROP_GAMMA(Videoio.CAP_PROP_GAMMA),
        CAP_PROP_TEMPERATURE(Videoio.CAP_PROP_TEMPERATURE),
        CAP_PROP_TRIGGER(Videoio.CAP_PROP_TRIGGER),
        CAP_PROP_TRIGGER_DELAY(Videoio.CAP_PROP_TRIGGER_DELAY),
        CAP_PROP_WHITE_BALANCE_RED_V(Videoio.CAP_PROP_WHITE_BALANCE_RED_V),
        CAP_PROP_ZOOM(Videoio.CAP_PROP_ZOOM),
        CAP_PROP_FOCUS(Videoio.CAP_PROP_FOCUS),
        CAP_PROP_GUID(Videoio.CAP_PROP_GUID),
        CAP_PROP_ISO_SPEED(Videoio.CAP_PROP_ISO_SPEED),
        CAP_PROP_BACKLIGHT(Videoio.CAP_PROP_BACKLIGHT),
        CAP_PROP_PAN(Videoio.CAP_PROP_PAN),
        CAP_PROP_TILT(Videoio.CAP_PROP_TILT),
        CAP_PROP_ROLL(Videoio.CAP_PROP_ROLL),
        CAP_PROP_IRIS(Videoio.CAP_PROP_IRIS),
        CAP_PROP_SETTINGS(Videoio.CAP_PROP_SETTINGS), // ! Pop up video/camera filter dialog (note: only supported by DSHOW
                               // backend currently. Property value is ignored)
        CAP_PROP_BUFFERSIZE(Videoio.CAP_PROP_BUFFERSIZE),
        CAP_PROP_AUTOFOCUS(Videoio.CAP_PROP_AUTOFOCUS);
        
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

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

import org.apache.commons.logging.Log;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
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
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A Camera implementation based on the OpenCV FrameGrabbers.
 */
public class OpenCvRemoteCamera extends ReferenceCamera implements Runnable {
    static {
        nu.pattern.OpenCV.loadLocally();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    @Attribute(name = "deviceIndex", required = true)
    private String deviceURI = "";

    @Attribute(required = false)
    private int preferredWidth = 800;
    @Attribute(required = false)
    private int preferredHeight = 600;
    @Attribute(required = false)
    private int fps = 24;

    private VideoCapture fg = new VideoCapture();
    private Thread thread;
    private boolean dirty = false;
    private boolean reachable = false;

    public OpenCvRemoteCamera() {}

    @Override
    public synchronized BufferedImage internalCapture() {
        if (thread == null) {
            initCamera();
        }
        Mat mat = new Mat();
        try {
            if (fg.retrieve(mat, Videoio.CAP_FFMPEG)) {
                reachable = true;
            } else {
                reachable = false;
                mat = new Mat(preferredHeight, preferredWidth, CvType.CV_8UC3);
                Imgproc.line(mat, new Point(0, 0), new Point(preferredWidth, preferredHeight), new Scalar(255, 0, 255, 0), 2);
                Imgproc.line(mat, new Point(0, preferredHeight), new Point(preferredWidth, 0), new Scalar(255, 0, 255, 0), 2);
            }
            BufferedImage img = OpenCvUtils.toBufferedImage(mat);
            return transformImage(img);
        }
        catch (Exception e) {
            e.printStackTrace();
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
                } else {
                    Logger.warn("No image!");
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

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
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
}

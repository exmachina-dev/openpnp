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

import org.openpnp.CameraListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.wizards.CameraConfigurationWizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.HttpCameraConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeSupport;
import java.net.URL;

public class HttpCamera extends ReferenceCamera implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(HttpCamera.class);

    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    @Attribute(required = false)
    private int refreshInterval = 500;

    @Element
    private String sourceUrl;

    @Attribute(required = false)
    private int width = 640;

    @Attribute(required = false)
    private int height = 480;

    private BufferedImage source;

    private Thread thread;

    public HttpCamera() {
        unitsPerPixel = new Location(LengthUnit.Inches, 0.04233, 0.04233, 0, 0);
    }

    @SuppressWarnings("unused")
    @Commit
    private void commit() throws Exception {
        setSourceUrl(sourceUrl);
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener, int maximumFps) {
        start();
        super.startContinuousCapture(listener, maximumFps);
    }

    @Override
    public synchronized void stopContinuousCapture(CameraListener listener) {
        super.stopContinuousCapture(listener);
        if (listeners.size() == 0) {
            stop();
        }
    }

    private synchronized void stop() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            try {
                thread.join();
            }
            catch (Exception e) {

            }
            thread = null;
        }
    }

    private synchronized void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) throws Exception {
        String oldValue = this.sourceUrl;
        this.sourceUrl = sourceUrl;
        pcs.firePropertyChange("sourceUrl", oldValue, sourceUrl);
        initialize();
    }
    public int getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(int refreshInterval) throws Exception {
        int oldValue = this.refreshInterval;
        this.refreshInterval = refreshInterval;
        pcs.firePropertyChange("refreshInterval", oldValue, refreshInterval);
    }

    @Override
    public synchronized BufferedImage capture() {
        /*
         * Create a buffer and draw image in it
         */

        BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics gFrame = frame.getGraphics();

        try {
            source = ImageIO.read(new URL(sourceUrl));
            gFrame.drawImage(source, 0, 0, source.getWidth(), source.getHeight(), null);
        } catch (Exception e) {
            gFrame.drawString("Unable to get image from", 100, 20);
            gFrame.drawString(sourceUrl, 100, 40);
            gFrame.drawLine(0, 0, width, height);
            gFrame.drawLine(0, height, width, 0);
        }

        gFrame.dispose();

        return transformImage(frame);
    }

    private synchronized void initialize() throws Exception {
        stop();

        if (listeners.size() > 0) {
            start();
        }
    }


    public void run() {
        while (!Thread.interrupted()) {
            BufferedImage frame = capture();
            broadcastCapture(frame);
            try {
                Thread.sleep(refreshInterval);
            }
            catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new HttpCameraConfigurationWizard(this);
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

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(new CameraConfigurationWizard(this)),
                new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }
}

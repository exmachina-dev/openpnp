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

package org.openpnp.machine.reference.camera.wizards;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.camera.HttpCamera;
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.machine.reference.wizards.ReferenceCameraConfigurationWizard;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;

@SuppressWarnings("serial")
public class HttpCameraConfigurationWizard extends ReferenceCameraConfigurationWizard {
    private final HttpCamera camera;

    private JPanel panelGeneral;
    private JLabel lblSourceUrl;
    private JLabel lblRefreshInterval;
    private JTextField textFieldSourceUrl;
    private JTextField textFieldRefreshInterval;

    public HttpCameraConfigurationWizard(HttpCamera camera) {
        super(camera);

        this.camera = camera;

        panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "General", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelGeneral.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow")},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        lblSourceUrl = new JLabel("Source URL");
        panelGeneral.add(lblSourceUrl, "2, 2, right, default");

        textFieldSourceUrl = new JTextField();
        panelGeneral.add(textFieldSourceUrl, "4, 2, fill, default");
        textFieldSourceUrl.setColumns(10);

        lblRefreshInterval = new JLabel("Refresh interval");
        panelGeneral.add(lblRefreshInterval, "2, 4, right, default");

        textFieldRefreshInterval = new JTextField();
        textFieldRefreshInterval.setToolTipText("Specify wait time between refresh (in milliseconds)");
        panelGeneral.add(textFieldRefreshInterval, "4, 4, left, default");
        textFieldRefreshInterval.setColumns(8);
    }

    @Override
    public void createBindings() {
        super.createBindings();
        IntegerConverter integerConverter = new IntegerConverter();
        addWrappedBinding(camera, "sourceUrl", textFieldSourceUrl, "text");
        addWrappedBinding(camera, "refreshInterval", textFieldRefreshInterval, "text", integerConverter);
        ComponentDecorators.decorateWithAutoSelect(textFieldSourceUrl);
        ComponentDecorators.decorateWithAutoSelect(textFieldRefreshInterval);
    }
}

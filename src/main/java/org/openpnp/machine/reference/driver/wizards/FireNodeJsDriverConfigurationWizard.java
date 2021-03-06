package org.openpnp.machine.reference.driver.wizards;

import com.jgoodies.forms.layout.*;
import org.json.JSONObject;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.driver.FireNodeJsDriver;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class FireNodeJsDriverConfigurationWizard extends AbstractConfigurationWizard {
    private final FireNodeJsDriver driver;
    private JComboBox<String> comboBoxProtocol;
    private JTextField textFieldHost;
    private JTextField textFieldPort;
    private JTextArea beforeResetConfig;
    private JSpinner endEffectorLedRingPin;
    private JSpinner upLookingLedRingPin;
    private JSpinner vacuumPin;
    private JCheckBox invertMotorX;
    private JCheckBox invertMotorY;
    private JCheckBox invertMotorZ;
    private JCheckBox invertAxisX;
    private JCheckBox invertAxisY;
    private JCheckBox disableLppForShortMoves;
    private JCheckBox disableLpp;
    private JSpinner homeLppSpinner;
    private JTextField lppSpeedField;
    private JTextField lppZField;
    private JTextField msSettleField;
    private JCheckBox invertVacuumPin;
    private JCheckBox powerSupplyCheckBox;
    private JCheckBox sendBeforeResetConfig;
    private JSpinner powerSupplyPin;

    public FireNodeJsDriverConfigurationWizard(FireNodeJsDriver driver) {
        this.driver = driver;

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Connection panel
        JPanel panelConnection = new JPanel();
        panelConnection.setBorder(new TitledBorder(null, "Connection settings", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelConnection);

        panelConnection.setLayout(new FormLayout(
                new ColumnSpec[] {
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblProtocolName = new JLabel("Protocol");
        panelConnection.add(lblProtocolName, "2, 2, center, default");

        comboBoxProtocol = new JComboBox<String>();
        panelConnection.add(comboBoxProtocol, "2, 4, fill, default");

        JLabel lblSpacerProtocolHost = new JLabel("://");
        panelConnection.add(lblSpacerProtocolHost, "4, 4, center, default");

        JLabel lblHost = new JLabel("Host");
        panelConnection.add(lblHost, "6, 2, center, default");

        textFieldHost = new JTextField();
        textFieldHost.setColumns(20);
        panelConnection.add(textFieldHost, "6, 4, fill, default");

        JLabel lblSpacerHostPort = new JLabel(":");
        panelConnection.add(lblSpacerHostPort, "8, 4, center, default");

        JLabel lblPort = new JLabel("Port");
        panelConnection.add(lblPort, "10, 2, center, default");

        textFieldPort = new JTextField();
        textFieldPort.setColumns(5);
        panelConnection.add(textFieldPort, "10, 4, fill, default");

        comboBoxProtocol.addItem(new String("http"));
        comboBoxProtocol.addItem(new String("https"));

        // Motor panel
        JPanel panelMotors = new JPanel();
        panelMotors.setBorder(new TitledBorder(null, "Motors", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelMotors);
        panelMotors
                .setLayout(
                        new FormLayout(
                                new ColumnSpec[] {
                                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(1)"),
                                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(3)"),
                                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(3)"),
                                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(3)"),
                                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(5)"),
                                },
                                new RowSpec[] {
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblMotor = new JLabel("Motors");
        panelMotors.add(lblMotor, "2, 4, right, default");

        JLabel label = new JLabel("Invert X");
        panelMotors.add(label, "4, 2, center, center");

        JLabel label_1 = new JLabel("Invert Y");
        panelMotors.add(label_1, "6, 2, center, center");

        JLabel label_2 = new JLabel("Invert Z");
        panelMotors.add(label_2, "8, 2, center, center");

        invertMotorX = new JCheckBox();
        panelMotors.add(invertMotorX, "4, 4, center, center");

        invertMotorY = new JCheckBox();
        panelMotors.add(invertMotorY, "6, 4, center, center");

        invertMotorZ = new JCheckBox();
        panelMotors.add(invertMotorZ, "8, 4, center, center");

        JLabel lblAxis = new JLabel("Axis");
        panelMotors.add(lblAxis, "2, 8, right, default");

        JLabel lblInvertAxisX = new JLabel("Invert X");
        panelMotors.add(lblInvertAxisX, "4, 6, center, center");

        JLabel lblInvertAxisY = new JLabel("Invert Y");
        panelMotors.add(lblInvertAxisY, "6, 6, center, center");

        invertAxisX = new JCheckBox();
        panelMotors.add(invertAxisX, "4, 8, center, center");

        invertAxisY = new JCheckBox();
        panelMotors.add(invertAxisY, "6, 8, center, center");

        // LPP panel
        JPanel panelLPP = new JPanel();
        panelLPP.setBorder(new TitledBorder(null, "Long Path Precision", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelLPP);
        panelLPP
                .setLayout(
                        new FormLayout(
                                new ColumnSpec[] {
                                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(3)"),
                                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(3)"),
                                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(3)"),
                                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(5)"),
                                },
                                new RowSpec[] {
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblDisableLpp = new JLabel("Disable LPP");
        panelLPP.add(lblDisableLpp, "2, 2, right, center");

        disableLpp = new JCheckBox();
        panelLPP.add(disableLpp, "4, 2, left, center");

        JLabel lblDisableLpp2 = new JLabel("Disable LPP for short moves");
        panelLPP.add(lblDisableLpp2, "6, 2, right, center");

        disableLppForShortMoves = new JCheckBox();
        panelLPP.add(disableLppForShortMoves, "8, 2, left, center");

        JLabel lblHomeLpp = new JLabel("Home after moves");
        panelLPP.add(lblHomeLpp, "2, 4, right, default");

        homeLppSpinner = new JSpinner();
        ((JSpinner.DefaultEditor) homeLppSpinner.getEditor()).getTextField().setColumns(4);
        panelLPP.add(homeLppSpinner, "4, 4, left, center");

        JLabel lblLppSpeed = new JLabel("LPP speed");
        panelLPP.add(lblLppSpeed, "6, 4, right, center");

        lppSpeedField = new JTextField();
        lppSpeedField.setColumns(4);
        lppSpeedField.setToolTipText("0..1 in float");
        panelLPP.add(lppSpeedField, "8, 4, left, center");

        JLabel lblMsSettle = new JLabel("Settle time");
        panelLPP.add(lblMsSettle, "2, 6, right, center");

        msSettleField = new JTextField();
        msSettleField.setColumns(5);
        msSettleField.setToolTipText("in milliseconds");
        panelLPP.add(msSettleField, "4, 6, left, center");

        JLabel lblLppZ = new JLabel("LPP Z");
        panelLPP.add(lblLppZ, "6, 6, right, center");

        lppZField = new JTextField();
        lppZField.setColumns(3);
        panelLPP.add(lppZField, "8, 6, left, center");

        // Pins configuration panel
        JPanel panelPins = new JPanel();
        panelPins.setBorder(new TitledBorder(null, "Pin configuration", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelPins);
        panelPins.setLayout(
                new FormLayout(
                        new ColumnSpec[] {
                                FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(3)"),
                                FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(3)"),
                                FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(3)"),
                                FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(3)"),
                        },
                        new RowSpec[] {
                                FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblPowerManagement = new JLabel("Power Management");
        panelPins.add(lblPowerManagement, "2, 2, right, default");

        powerSupplyCheckBox = new JCheckBox("Enable");
        panelPins.add(powerSupplyCheckBox, "4, 2, left, center");

        JLabel lblPowerSupply = new JLabel("Power supply pin");
        panelPins.add(lblPowerSupply, "2, 4, right, default");

        powerSupplyPin = new JSpinner();
        ((JSpinner.DefaultEditor) powerSupplyPin.getEditor()).getTextField().setColumns(4);
        panelPins.add(powerSupplyPin, "4, 4, left, center");

        JLabel lblVacuumPin = new JLabel("Vacuum pin");
        panelPins.add(lblVacuumPin, "6, 2, right, default");

        vacuumPin = new JSpinner();
        ((JSpinner.DefaultEditor) vacuumPin.getEditor()).getTextField().setColumns(4);
        panelPins.add(vacuumPin, "8, 2, left, center");

        JLabel lblInvertVacuumPin = new JLabel("Invert vacuum pin");
        panelPins.add(lblInvertVacuumPin, "6, 4, right, default");

        invertVacuumPin = new JCheckBox();
        panelPins.add(invertVacuumPin, "8, 4, left, center");

        JLabel lblEndEffectorLedRing = new JLabel("End effector led ring pin");
        panelPins.add(lblEndEffectorLedRing, "2, 6, right, default");

        endEffectorLedRingPin = new JSpinner();
        ((JSpinner.DefaultEditor) endEffectorLedRingPin.getEditor()).getTextField().setColumns(4);
        panelPins.add(endEffectorLedRingPin, "4, 6, left, center");

        JLabel lblUpLookingLedRing = new JLabel("Up-looking led ring pin");
        panelPins.add(lblUpLookingLedRing, "6, 6, right, default");

        upLookingLedRingPin = new JSpinner();
        ((JSpinner.DefaultEditor) upLookingLedRingPin.getEditor()).getTextField().setColumns(4);
        panelPins.add(upLookingLedRingPin, "8, 6, left, center");

        // Advanced configuration
        JPanel panelAdvancedConfig = new JPanel();
        panelAdvancedConfig.setBorder(
                new TitledBorder(null, "Advanced configuration", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelAdvancedConfig);
        panelAdvancedConfig.setLayout(new FormLayout(
                new ColumnSpec[] {
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(1)"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(3)"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(3)"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(3)"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow(5)"),
                },
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblSendBeforeResetConfig = new JLabel("Send config before reset");
        panelAdvancedConfig.add(lblSendBeforeResetConfig, "2, 2, right, default");

        sendBeforeResetConfig = new JCheckBox();
        panelAdvancedConfig.add(sendBeforeResetConfig, "4, 2, left, center");

        JLabel lblBeforeResetConfig = new JLabel("Reset config");
        lblBeforeResetConfig.setToolTipText("This string is send at start. This allow the machine parameters to be specified. Especially useful for machines without EEPROM.");
        panelAdvancedConfig.add(lblBeforeResetConfig, "2, 4, right, default");

        beforeResetConfig = new JTextArea();
        beforeResetConfig.setRows(5);
        beforeResetConfig.setColumns(100);
        panelAdvancedConfig.add(beforeResetConfig, "4, 4");
    }

    @Override
    public void createBindings() {
        IntegerConverter integerConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter("%f");

        addWrappedBinding(driver, "protocol", comboBoxProtocol, "selectedItem");
        addWrappedBinding(driver, "host", textFieldHost, "text");
        addWrappedBinding(driver, "port", textFieldPort, "text", integerConverter);
        addWrappedBinding(driver, "invertMotorX", invertMotorX, "selected");
        addWrappedBinding(driver, "invertMotorY", invertMotorY, "selected");
        addWrappedBinding(driver, "invertMotorZ", invertMotorZ, "selected");

        addWrappedBinding(driver, "invertAxisX", invertAxisX, "selected");
        addWrappedBinding(driver, "invertAxisY", invertAxisY, "selected");

        addWrappedBinding(driver, "disableLppForShortMoves", disableLppForShortMoves, "selected");
        addWrappedBinding(driver, "disableLpp", disableLpp, "selected");
        addWrappedBinding(driver, "homeLPP", homeLppSpinner, "value");
        addWrappedBinding(driver, "lppSpeed", lppSpeedField, "text", doubleConverter);
        addWrappedBinding(driver, "lppZ", lppZField, "text", doubleConverter);
        addWrappedBinding(driver, "msSettle", msSettleField, "text", integerConverter);

        addWrappedBinding(driver, "powerSupplyManagement", powerSupplyCheckBox, "selected");
        addWrappedBinding(driver, "powerSupplyPin", powerSupplyPin, "value");

        addWrappedBinding(driver, "vacuumPin", vacuumPin, "value");
        addWrappedBinding(driver, "invertVacuumPin", invertVacuumPin, "selected");

        addWrappedBinding(driver, "endEffectorLedRingPin", endEffectorLedRingPin, "value");
        addWrappedBinding(driver, "upLookingLedRingPin", upLookingLedRingPin, "value");

        addWrappedBinding(driver, "sendBeforeResetConfig", sendBeforeResetConfig, "selected");
        addWrappedBinding(driver, "beforeResetConfig", beforeResetConfig, "text");
    }
}

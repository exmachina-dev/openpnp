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
    private JSpinner vacuumPumpPin;
    private JCheckBox invertMotorX;
    private JCheckBox invertMotorY;
    private JCheckBox invertMotorZ;
    private JCheckBox powerSupplyCheckBox;
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
        panelConnection.add(textFieldHost, "6, 4, fill, default");

        JLabel lblSpacerHostPort = new JLabel(":");
        panelConnection.add(lblSpacerHostPort, "8, 4, center, default");

        JLabel lblPort = new JLabel("Port");
        panelConnection.add(lblPort, "10, 2, center, default");

        textFieldPort = new JTextField();
        panelConnection.add(textFieldPort, "10, 4, fill, default");

        comboBoxProtocol.addItem(new String("http"));
        comboBoxProtocol.addItem(new String("https"));

        // Motor panel
        JPanel panelMotors = new JPanel();
        panelMotors.setBorder(new TitledBorder(null, "Motors and actuators", TitledBorder.LEADING,
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

        JLabel lblAxis = new JLabel("Motors");
        panelMotors.add(lblAxis, "2, 4, right, default");

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

        JLabel lblPowerManagement = new JLabel("Power Management");
        panelMotors.add(lblPowerManagement, "2, 6, right, default");

        powerSupplyCheckBox = new JCheckBox("Enable");
        panelMotors.add(powerSupplyCheckBox, "4, 6, left, center");

        JLabel lblPowerSupply = new JLabel("Power supply pin");
        panelMotors.add(lblPowerSupply, "6, 6, right, default");

        powerSupplyPin = new JSpinner();
        powerSupplyPin.setSize(100, 0);
        panelMotors.add(powerSupplyPin, "8, 6, left, center");

        JLabel lblVacuumPump = new JLabel("Vacuum pump pin");
        panelMotors.add(lblVacuumPump, "2, 8, right, default");

        vacuumPumpPin = new JSpinner();
        vacuumPumpPin.setSize(100, 100);
        panelMotors.add(vacuumPumpPin, "4, 8, left, center");

        JLabel lblEndEffectorLedRing = new JLabel("End effector led ring pin");
        panelMotors.add(lblEndEffectorLedRing, "2, 10, right, default");

        endEffectorLedRingPin = new JSpinner();
        panelMotors.add(endEffectorLedRingPin, "4, 10, left, center");

        JLabel lblUpLookingLedRing = new JLabel("Up-looking led ring pin");
        panelMotors.add(lblUpLookingLedRing, "6, 10, right, default");

        upLookingLedRingPin = new JSpinner();
        panelMotors.add(upLookingLedRingPin, "8, 10, left, center");

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

        JLabel lblBeforeResetConfig = new JLabel("Reset config");
        lblBeforeResetConfig.setToolTipText("This string is send a start. This allow the machine parameters to be specified. Especially useful for machines without EEPROM.");
        panelAdvancedConfig.add(lblBeforeResetConfig, "2, 2, right, default");

        beforeResetConfig = new JTextArea();
        beforeResetConfig.setRows(5);
        beforeResetConfig.setColumns(100);
        panelAdvancedConfig.add(beforeResetConfig, "4, 2");
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

        addWrappedBinding(driver, "powerSupplyManagement", powerSupplyCheckBox, "selected");
        addWrappedBinding(driver, "powerSupplyPin", powerSupplyPin, "value");
        addWrappedBinding(driver, "vacuumPumpPin", vacuumPumpPin, "value");
        addWrappedBinding(driver, "endEffectorLedRingPin", endEffectorLedRingPin, "value");
        addWrappedBinding(driver, "upLookingLedRingPin", upLookingLedRingPin, "value");

        addWrappedBinding(driver, "beforeResetConfig", beforeResetConfig, "text");
    }


    public class ConfigProxy {
        // [1ma] m1 map to axis 0 [0=X,1=Y,2=Z...]
        // [1sa] m1 step angle 1.800 deg
        // [1tr] m1 travel per revolution 1.250 mm
        // [1mi] m1 microsteps 8 [1,2,4,8]
        // [1po] m1 polarity 0 [0=normal,1=reverse]
        // [1pm] m1 power management 1 [0=off,1=on]
        // tinyg [mm] ok>

        // [xam] x axis mode 1 [standard]
        // [xvm] x velocity maximum 5000.000 mm/min
        // [xfr] x feedrate maximum 5000.000 mm/min
        // [xtm] x travel maximum 150.000 mm
        // [xjm] x jerk maximum 20000000 mm/min^3
        // [xjh] x jerk homing 20000000 mm/min^3
        // [xjd] x junction deviation 0.0500 mm (larger is faster)
        // [xsn] x switch min 1 [0=off,1=homing,2=limit,3=limit+homing]
        // [xsx] x switch max 0 [0=off,1=homing,2=limit,3=limit+homing]
        // [xsv] x search velocity 500.000 mm/min
        // [xlv] x latch velocity 100.000 mm/min
        // [xlb] x latch backoff 2.000 mm
        // [xzb] x zero backoff 1.000 mm
        // tinyg [mm] ok>


        public double getStepAngleM1() throws Exception {
            return getConfigDouble("1sa");
        }

        public void setStepAngleM1(double v) throws Exception {
            setConfigDouble("1sa", v);
        }

        public double getStepAngleM2() throws Exception {
            return getConfigDouble("2sa");
        }

        public void setStepAngleM2(double v) throws Exception {
            setConfigDouble("2sa", v);
        }

        public double getStepAngleM3() throws Exception {
            return getConfigDouble("3sa");
        }

        public void setStepAngleM3(double v) throws Exception {
            setConfigDouble("3sa", v);
        }

        public double getStepAngleM4() throws Exception {
            return getConfigDouble("4sa");
        }

        public void setStepAngleM4(double v) throws Exception {
            setConfigDouble("4sa", v);
        }



        public double getTravelPerRevM1() throws Exception {
            return getConfigDouble("1tr");
        }

        public void setTravelPerRevM1(double v) throws Exception {
            setConfigDouble("1tr", v);
        }

        public double getTravelPerRevM2() throws Exception {
            return getConfigDouble("2tr");
        }

        public void setTravelPerRevM2(double v) throws Exception {
            setConfigDouble("2tr", v);
        }

        public double getTravelPerRevM3() throws Exception {
            return getConfigDouble("3tr");
        }

        public void setTravelPerRevM3(double v) throws Exception {
            setConfigDouble("3tr", v);
        }

        public double getTravelPerRevM4() throws Exception {
            return getConfigDouble("4tr");
        }

        public void setTravelPerRevM4(double v) throws Exception {
            setConfigDouble("4tr", v);
        }



        public boolean getPolarityReversedM1() throws Exception {
            return getConfigBoolean("1po");
        }

        public void setPolarityReversedM1(boolean v) throws Exception {
            setConfigBoolean("1po", v);
        }

        public boolean getPolarityReversedM2() throws Exception {
            return getConfigBoolean("2po");
        }

        public void setPolarityReversedM2(boolean v) throws Exception {
            setConfigBoolean("2po", v);
        }

        public boolean getPolarityReversedM3() throws Exception {
            return getConfigBoolean("3po");
        }

        public void setPolarityReversedM3(boolean v) throws Exception {
            setConfigBoolean("3po", v);
        }

        public boolean getPolarityReversedM4() throws Exception {
            return getConfigBoolean("4po");
        }

        public void setPolarityReversedM4(boolean v) throws Exception {
            setConfigBoolean("4po", v);
        }



        public boolean getPowerMgmtM1() throws Exception {
            return getConfigBoolean("1pm");
        }

        public void setPowerMgmtM1(boolean v) throws Exception {
            setConfigBoolean("1pm", v);
        }

        public boolean getPowerMgmtM2() throws Exception {
            return getConfigBoolean("2pm");
        }

        public void setPowerMgmtM2(boolean v) throws Exception {
            setConfigBoolean("2pm", v);
        }

        public boolean getPowerMgmtM3() throws Exception {
            return getConfigBoolean("3pm");
        }

        public void setPowerMgmtM3(boolean v) throws Exception {
            setConfigBoolean("3pm", v);
        }

        public boolean getPowerMgmtM4() throws Exception {
            return getConfigBoolean("4pm");
        }

        public void setPowerMgmtM4(boolean v) throws Exception {
            setConfigBoolean("4pm", v);
        }



        public int getVelMaxX() throws Exception {
            return getConfigInt("xvm");
        }

        public void setVelMaxX(int v) throws Exception {
            setConfigInt("xvm", v);
        }

        public int getVelMaxY() throws Exception {
            return getConfigInt("yvm");
        }

        public void setVelMaxY(int v) throws Exception {
            setConfigInt("yvm", v);
        }



        public int getFeedMaxX() throws Exception {
            return getConfigInt("xfr");
        }

        public void setFeedMaxX(int v) throws Exception {
            setConfigInt("xfr", v);
        }

        public int getFeedMaxY() throws Exception {
            return getConfigInt("yfr");
        }

        public void setFeedMaxY(int v) throws Exception {
            setConfigInt("yfr", v);
        }



        public int getJerkMaxX() throws Exception {
            return getConfigInt("xjm");
        }

        public void setJerkMaxX(int v) throws Exception {
            setConfigInt("xjm", v);
        }

        public int getJerkMaxY() throws Exception {
            return getConfigInt("yjm");
        }

        public void setJerkMaxY(int v) throws Exception {
            setConfigInt("yjm", v);
        }



        // TODO: Check for response errors in these methods.
        private int getConfigInt(String name) throws Exception {
            JSONObject o = driver.sendCommand("/firestep", name).getBody().getObject().getJSONObject("r");
            return o.getInt(name);
        }

        private void setConfigInt(String name, int v) throws Exception {
            driver.sendCommand("/firestep", new JSONObject().put(name, v));
        }

        private double getConfigDouble(String name) throws Exception {
            JSONObject o = driver.sendCommand("/firestep", name).getBody().getObject().getJSONObject("r");
            return o.getDouble(name);
        }

        private void setConfigDouble(String name, double v) throws Exception {
            driver.sendCommand("/firestep", new JSONObject().put(name, v));
        }

        private boolean getConfigBoolean(String name) throws Exception {
            return getConfigInt(name) == 1;
        }

        private void setConfigBoolean(String name, boolean v) throws Exception {
            setConfigInt(name, v ? 1 : 0);
        }
    }
}

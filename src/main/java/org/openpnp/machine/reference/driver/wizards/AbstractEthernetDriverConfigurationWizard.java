package org.openpnp.machine.reference.driver.wizards;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JSpinner;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.driver.AbstractEthernetDriver;
import org.openpnp.gui.support.IntegerConverter;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class AbstractEthernetDriverConfigurationWizard extends AbstractConfigurationWizard {
    private final AbstractEthernetDriver driver;
    private JComboBox<String> comboBoxProtocol;
    private JTextField textFieldHost;
    private JTextField textFieldPort;

    public AbstractEthernetDriverConfigurationWizard(AbstractEthernetDriver driver) {
        this.driver = driver;

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblProtocolName = new JLabel("Protocol");
        panel.add(lblProtocolName, "2, 2, right, default");

        comboBoxProtocol = new JComboBox<String>();
        panel.add(comboBoxProtocol, "4, 2, fill, default");

        JLabel lblHost = new JLabel("Host");
        panel.add(lblHost, "2, 4, right, default");

        textFieldHost = new JTextField();
        panel.add(textFieldHost, "4, 4, fill, default");

        JLabel lblPort = new JLabel("Port");
        panel.add(lblPort, "2, 6, right, default");

        textFieldPort = new JTextField();
        panel.add(textFieldPort, "4, 6, fill, default");
        
        comboBoxProtocol.addItem(new String("http"));
        comboBoxProtocol.addItem(new String("https"));
    }

    @Override
    public void createBindings() {
    	IntegerConverter integerConverter = new IntegerConverter();
    	
        addWrappedBinding(driver, "protocol", comboBoxProtocol, "selectedItem");
        addWrappedBinding(driver, "host", textFieldHost, "text");
        addWrappedBinding(driver, "port", textFieldPort, "text", integerConverter);
    }
}

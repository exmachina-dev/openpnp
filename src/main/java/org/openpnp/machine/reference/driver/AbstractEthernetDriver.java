package org.openpnp.machine.reference.driver;

import java.io.Closeable;
import java.io.IOException;

import javax.swing.Icon;

import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.model.Location;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.unirest.http.Unirest;

/**
 * A base class for basic SerialPort based Drivers. Includes functions for connecting,
 * disconnecting, reading and sending lines.
 */
public abstract class AbstractEthernetDriver implements ReferenceDriver, Closeable {
    private static final Logger logger = LoggerFactory.getLogger(AbstractEthernetDriver.class);

    @Attribute(required = false)
    protected String protocol;
    @Attribute(required = false)
    protected String host;
    @Attribute(required = false)
    protected int port;
    @Attribute(required = false)
    protected int connectionTimeout = 1000; // 1s to etablish connection
    @Attribute(required = false)
    protected int socketTimeout = 100000; // 10s to receive data
    
    protected String hostUrl;

    protected synchronized void connect() throws Exception {
        this.setHostUrl(this.protocol, this.host, this.port);
        Unirest.setDefaultHeader("Accept", "application/json");
    }

    protected synchronized void disconnect() throws Exception {
        Unirest.shutdown();
    }

    @Override
    public void dispense(ReferencePasteDispenser dispenser, Location startLocation,
            Location endLocation, long dispenseTimeMilliseconds) throws Exception {
        // Do nothing. This is just stubbed in so that it can be released
        // without breaking every driver in the wild.
    }

    @Override
    public void close() throws IOException {
        try {
            disconnect();
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
    	this.protocol = protocol;
    }
    
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
    	this.host = host;
    }
    
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
    	this.port = port;
    }

    public String getHostURL() {
        return hostUrl;
    }

    public void setHostUrl(String protocol, String host, int port) {
    	if(port != 0) {
    		this.hostUrl = protocol + "://" + host + ":" + Integer.toString(port);
    	} else {
    		this.hostUrl = protocol + "://" + host;
    	}
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        // TODO Auto-generated method stub
        return null;
    }
}


/*
 * Copyright (C) 2016 Benoit Rapidel <benoit.rapidel@exmachina.fr>
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

package org.openpnp.machine.reference.driver;

import java.util.Collection;

import javax.swing.Action;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.driver.wizards.AbstractEthernetDriverConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.BaseRequest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.MultipartBody;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.json.JSONArray;

public class FireNodeJsDriver extends AbstractEthernetDriver {
    private static final Logger logger = LoggerFactory.getLogger(FireNodeJsDriver.class);
    private static final int minimumRequiredVersion = 4; // Version is major * 1000 + minor. Patch number is no checked.

    @Attribute(required = false)
    private double feedRateMmPerMinute = 5000;
    @Element(required = false)
    private Location homeLocation = new Location(LengthUnit.Millimeters);

    private double x, y, z, c;
    private Object commandLock = new Object();
    private Object movementWaitLock = new Object();
    private boolean connected;
    private int connectedVersion;

    public FireNodeJsDriver() {}

    @Override
    public synchronized void connect() throws Exception {
        super.connect();
        
        for (int i = 0; i < 1 && !connected; i++) {
            try {
                HttpResponse<String> helloResponse = sendCommandAsString("/firenodejs/hello");
                if (helloResponse.getStatus() != 200) {
                	throw new Exception(String.format("Unable to connect to {} ({})", hostUrl, helloResponse.getStatus()));
                } else {
                	logger.debug(String.format("%s said: %s", hostUrl, helloResponse.getBody()));
                }

                HttpResponse<JsonNode> response = sendCommand("/firenodejs/models");
                JSONObject v = response.getBody().getObject().getJSONObject("firenodejs").getJSONObject("version");
                connectedVersion = v.getInt("major") * 1000 + v.getInt("minor");
                String versionString = String.format("%d.%d.%d", v.getInt("major"), v.getInt("minor"), v.getInt("patch"));
                logger.debug("Firenodejs version: {}", versionString);
                connected = true;
                break;
            }
            catch (Exception e) {
                logger.debug("Firmware version check failed", e);
            }
        }

        if (!connected) {
            throw new Exception(String.format(
                    "Unable to receive connection response from FireNodeJs. Check your protocol, host and port and that you are running at least version %f of FireNodeJs.",
                    minimumRequiredVersion));
        }

        if (connectedVersion < minimumRequiredVersion) {
            throw new Exception(String.format(
                    "This driver requires FireNodeJs version %.2f or higher. You are running version %.2f",
                    minimumRequiredVersion, connectedVersion));
        } else {
        	logger.debug("Version check successful: %.2f", connectedVersion);
        }

        logger.debug(String.format("Connected to FireNodeJs Version: %.2f", connectedVersion));

        // We are connected to at least the minimum required version now
        // So perform some setup

        // Turn off the stepper drivers
        setEnabled(true);
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        if (enabled && !connected) {
            connect();
        }

        if (connected) {
	        // TODO: iod28 switches pin 28 which is the power supply pin on FPD from TW
	        // Should be configurable
	        sendCommand("/firestep", "iod28", enabled ? "1" : "0");
	        
	        // TODO: iod5 switches pin 5 which is the end effector led ring on FPD from TW
	        // Should be configurable
	        sendCommand("/firestep", "iod5", enabled ? "1" : "0");
        }
    }

    @Override
    public void home(ReferenceHead head) throws Exception {
        synchronized (movementWaitLock) {
            HttpResponse<JsonNode> response = sendCommand("/firestep", "hom");
            if (response.getStatus() == 0) {
                waitForMovementComplete();
            }
        }

        // TODO: This homeLocation really needs to be Head specific.
        Location homeLocation = this.homeLocation.convertToUnits(LengthUnit.Millimeters);
        JSONObject homeCoords = new JSONObject();
        homeCoords.put("x", homeLocation.getX());
        homeCoords.put("y", homeLocation.getY());
        homeCoords.put("z", homeLocation.getZ());
        homeCoords.put("a", homeLocation.getRotation());
        sendCommand("/firestep", "mov", homeCoords);
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed)
            throws Exception {
        location = location.subtract(hm.getHeadOffsets());

        location = location.convertToUnits(LengthUnit.Millimeters);

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        double c = location.getRotation();

        JSONObject newCoords = new JSONObject();
        newCoords.put("x", x);
        newCoords.put("y", y);
        newCoords.put("z", z);
        newCoords.put("a", c);
        HttpResponse<JsonNode> response = sendCommand("/firestep", "mov", newCoords);
        checkResponseCode(response);

        if (!Double.isNaN(x)) {
            this.x = x;
        }
        if (!Double.isNaN(y)) {
            this.y = y;
        }
        if (!Double.isNaN(z)) {
            this.z = z;
        }
        if (!Double.isNaN(c)) {
            this.c = c;
        }
    }

    @Override
    public void pick(ReferenceNozzle nozzle) throws Exception {
        sendCommand("/firepick/place");
    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {
        sendCommand("/firestep/pick");
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
        if (actuator.getIndex() == 0) {
            sendCommand("/firestep", String.format("iod{}", actuator.getIndex()), on ? 1 : 0);
        }
    }

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        return new Location(LengthUnit.Millimeters, x, y, z, c).add(hm.getHeadOffsets());
    }

    private void checkResponseCode(HttpResponse<JsonNode> o) {
    	if (o.getStatus() != 200) {
    		logger.error("Error in HTTP response: {}", o.getBody());
    	}
    	
    	if (o.getBody().getObject().getInt("s") != 0) {
    		String error = getStatusCodeDetails(o.getBody().getObject().getInt("s"));
    		logger.error("Error in response: {}", error);
    	}
    }

    public synchronized void disconnect() {
        connected = false;

        try {
            super.disconnect();
        }
        catch (Exception e) {
            logger.error("disconnect()", e);
        }
    }

    public synchronized HttpResponse<JsonNode> sendCommand(String command) throws Exception {
    	return sendCommand(Unirest.get(hostUrl + command));
    }
    
    public synchronized HttpResponse<JsonNode> sendCommand(String command, String field) throws Exception {
    	HttpRequestWithBody jsonCommand = Unirest.post(hostUrl + command);
    	
    	return sendCommand(jsonCommand.field(field, ""));
    }
    
    public synchronized HttpResponse<JsonNode> sendCommand(String command, String key, Object object) throws Exception {
    	HttpRequestWithBody jsonCommand = Unirest.post(hostUrl + command);
    	
    	return sendCommand(jsonCommand.field(key, object));
    }
    
    public synchronized HttpResponse<JsonNode> sendCommand(String command, String key, Collection<?> fields) throws Exception {
    	HttpRequestWithBody jsonCommand = Unirest.post(hostUrl + command);
    	
    	return sendCommand(jsonCommand.field(key, fields));
    }
    /*
    public synchronized HttpResponse<JsonNode> sendCommand(GetRequest command) throws Exception {
    	return sendCommand(command);
    }
    
    public synchronized HttpResponse<JsonNode> sendCommand(MultipartBody command) throws Exception {
    	return sendCommand(command);
    }*/

    public synchronized HttpResponse<JsonNode> sendCommand(BaseRequest command) throws Exception {
    	
    	if (hostUrl == null) {
    		logger.error("Host url is not defined");
    		throw new Exception("Host url is not defined");
    	}

    	HttpResponse<JsonNode> response;
        synchronized (commandLock) {
            logger.debug("sendCommand({})", command.getHttpRequest().getUrl());
            response = command.asJson();
            if (response.getStatus() == 200) {
            	logger.debug("OK");
            }
	        if (response.getHeaders().size() == 0) {
	            throw new Exception("Command did not return a response");
	        }
	
	        if (response.getStatus() != 200) {
	            throw new Exception("Request failed. Status code: " + response.getStatus());
	        }
        }
	    return response;
    }

    public synchronized HttpResponse<String> sendCommandAsString(String command) throws Exception {
    	
    	if (hostUrl == null) {
    		logger.error("Host url is not defined");
    		throw new Exception("Host url is not defined");
    	}

    	HttpResponse<String> response;
        synchronized (commandLock) {
        	HttpRequest c = Unirest.get(hostUrl + command);
            logger.debug("sendCommand({})", c.getHttpRequest().getUrl());
            response = c.asString();
            if (response.getStatus() == 200) {
            	logger.debug("success");
            }
	        if (response.getHeaders().size() == 0) {
	            throw new Exception("Command did not return a response");
	        }
	
	        if (response.getStatus() != 200) {
	            throw new Exception("Request failed. Status code: " + response.getStatus());
	        }
        }
	    return response;
    }

    // TODO: If no movement is happening this will never return. We may want to
    // have it issue a status report request now and then so it doesn't sit
    // forever.
    private void waitForMovementComplete() throws Exception {
        synchronized (movementWaitLock) {
            movementWaitLock.wait();
        }
    }

    private String getStatusCodeDetails(int statusCode) {
    	String r;
    	switch (statusCode) {
	    	case 0: r = "STATUS_OK"; 
	    	break;
	    	case 10: r = "STATUS_BUSY_PARSED"; 
	    	break;
	    	case 11: r = "STATUS_BUSY"; 
	    	break;
	    	case 12: r = "STATUS_BUSY_MOVING"; 
	    	break;
	    	case 13: r = "STATUS_BUSY_SETUP"; 
	    	break;
	    	case 14: r = "STATUS_BUSY_OK"; 
	    	break;
	    	case 15: r = "STATUS_BUSY_EEPROM"; 
	    	break;
	    	case 16: r = "STATUS_BUSY_CALIBRATING"; 
	    	break;
	    	case 20: r = "STATUS_WAIT_IDLE"; 
	    	break;
	    	case 21: r = "STATUS_WAIT_EOL"; 
	    	break;
	    	case 22: r = "STATUS_WAIT_CAMERA"; 
	    	break;
	    	case 23: r = "STATUS_WAIT_OPERATOR"; 
	    	break;
	    	case 24: r = "STATUS_WAIT_MOVING"; 
	    	break;
	    	case 25: r = "STATUS_WAIT_BUSY"; 
	    	break;
	    	case 26: r = "STATUS_WAIT_CANCELLED"; 
	    	break;
	    	case 27: r = "STATUS_WAIT_SLEEP"; 
	    	break;
	    	case -1: r = "STATUS_EMPTY"; 
	    	break;
	
	    	case -100: r = "STATUS_POSITION_ERROR"; 
	    	break;
	    	case -101: r = "STATUS_AXIS_ERROR"; 
	    	break;
	    	case -102: r = "STATUS_SYS_ERROR"; 
	    	break;
	    	case -103: r = "STATUS_S1_ERROR"; 
	    	break;
	    	case -104: r = "STATUS_S2_ERROR"; 
	    	break;
	    	case -105: r = "STATUS_S3_ERROR"; 
	    	break;
	    	case -106: r = "STATUS_S4_ERROR"; 
	    	break;
	    	case -112: r = "STATUS_MOTOR_INDEX"; 
	    	break;
	    	case -113: r = "STATUS_STEP_RANGE_ERROR"; 
	    	break;
	    	case -115: r = "STATUS_JSON_MEM1"; 
	    	break;
	    	case -116: r = "STATUS_JSON_MEM2"; 
	    	break;
	    	case -117: r = "STATUS_JSON_MEM3"; 
	    	break;
	    	case -118: r = "STATUS_JSON_MEM4"; 
	    	break;
	    	case -119: r = "STATUS_WAIT_ERROR"; 
	    	break;
	    	case -120: r = "STATUS_AXIS_DISABLED"; 
	    	break;
	    	case -121: r = "STATUS_NOPIN"; 
	    	break;
	    	case -129: r = "STATUS_MOTOR_ERROR"; 
	    	break;
	    	case -130: r = "STATUS_NOT_IMPLEMENTED"; 
	    	break;
	    	case -131: r = "STATUS_NO_MOTOR"; 
	    	break;
	    	case -132: r = "STATUS_PIN_CONFIG"; 
	    	break;
	    	case -133: r = "STATUS_VALUE_RANGE"; 
	    	break;
	    	case -134: r = "STATUS_STATE"; 
	    	break;
	    	case -135: r = "STATUS_CORE_PIN"; 
	    	break;
	    	case -136: r = "STATUS_NO_SUCH_PIN"; 
	    	break;
	    	case -137: r = "STATUS_EEPROM_ADDR"; 
	    	break;
	    	case -138: r = "STATUS_EEPROM_JSON"; 
	    	break;
	    	case -139: r = "STATUS_PROBE_PIN"; 
	    	break;
	    	case -140: r = "STATUS_KINEMATIC_XYZ"; 
	    	break;
	    	case -141: r = "STATUS_USER_EEPROM"; 
	    	break;
	    	case -142: r = "STATUS_CAL_HOME1"; 
	    	break;
	    	case -143: r = "STATUS_MARK_INDEX"; 
	    	break;
	    	case -144: r = "STATUS_MARK_AXIS"; 
	    	break;
	    	case -145: r = "STATUS_CAL_BED"; 
	    	break;
	    	case -146: r = "STATUS_UNKNOWN_PROGRAM"; 
	    	break;
	    	case -147: r = "STATUS_PROGRAM_SIZE"; 
	    	break;
	    	case -148: r = "STATUS_ZBOWL_GEAR"; 
	    	break;
	    	case -149: r = "STATUS_CAL_DEGREES"; 
	    	break;
	    	case -150: r = "STATUS_CAL_POSITION_0"; 
	    	break;
	    	case -151: r = "STATUS_DELTA_HOME"; 
	    	break;
	    	case -152: r = "STATUS_INVALID_Z"; 
	    	break;
	    	case -153: r = "STATUS_NO_EEPROM"; 
	    	break;
	
	    	case -200: r = "STATUS_STROKE_SEGPULSES"; 
	    	break;
	    	case -201: r = "STATUS_STROKE_END_ERROR"; 
	    	break;
	    	case -202: r = "STATUS_STROKE_MAXLEN"; 
	    	break;
	    	case -203: r = "STATUS_STROKE_TIME"; 
	    	break;
	    	case -204: r = "STATUS_STROKE_START"; 
	    	break;
	    	case -205: r = "STATUS_STROKE_NULL_ERROR"; 
	    	break;
	
	    	case -400: r = "STATUS_JSON_BRACE_ERROR"; 
	    	break;
	    	case -401: r = "STATUS_JSON_BRACKET_ERROR"; 
	    	break;
	    	case -402: r = "STATUS_UNRECOGNIZED_NAME"; 
	    	break;
	    	case -403: r = "STATUS_JSON_PARSE_ERROR"; 
	    	break;
	    	case -404: r = "STATUS_JSON_TOO_LONG"; 
	    	break;
	    	case -407: r = "STATUS_JSON_OBJECT"; 
	    	break;
	    	case -408: r = "STATUS_JSON_POSITIVE"; 
	    	break;
	    	case -409: r = "STATUS_JSON_POSITIVE1"; 
	    	break;
	    	case -410: r = "STATUS_JSON_KEY"; 
	    	break;
	    	case -411: r = "STATUS_JSON_STROKE_ERROR"; 
	    	break;
	    	case -412: r = "STATUS_RANGE_ERROR"; 
	    	break;
	    	case -413: r = "STATUS_S1S2LEN_ERROR"; 
	    	break;
	    	case -414: r = "STATUS_S1S3LEN_ERROR"; 
	    	break;
	    	case -415: r = "STATUS_S1S4LEN_ERROR"; 
	    	break;
	    	case -416: r = "STATUS_FIELD_ERROR"; 
	    	break;
	    	case -417: r = "STATUS_FIELD_RANGE_ERROR"; 
	    	break;
	    	case -418: r = "STATUS_FIELD_ARRAY_ERROR"; 
	    	break;
	    	case -419: r = "STATUS_FIELD_REQUIRED"; 
	    	break;
	    	case -420: r = "STATUS_JSON_ARRAY_LEN"; 
	    	break;
	    	case -421: r = "STATUS_OUTPUT_FIELD"; 
	    	break;
	    	case -422: r = "STATUS_FIELD_HEX_ERROR"; 
	    	break;
	    	case -423: r = "STATUS_JSON_CMD"; 
	    	break;
	    	case -424: r = "STATUS_JSON_STRING"; 
	    	break;
	    	case -425: r = "STATUS_JSON_EEPROM"; 
	    	break;
	    	case -426: r = "STATUS_JSON_EXEC"; 
	    	break;
	    	case -427: r = "STATUS_JSON_BOOL"; 
	    	break;
	    	case -428: r = "STATUS_JSON_255"; 
	    	break;
	    	case -429: r = "STATUS_JSON_DIGIT"; 
	    	break;
	    	case -430: r = "STATUS_MTO_FIELD"; 
	    	break;
	    	case -431: r = "STATUS_NO_MOCK"; 
	    	break;
	
	    	case -500: r = "STATUS_OPEN"; 
	    	break;
	    	case -501: r = "STATUS_IFIRESTEP"; 
	    	break;
	    	case -502: r = "STATUS_USB_OPEN"; 
	    	break;
	    	case -503: r = "STATUS_USB_CLOSE"; 
	    	break;
	    	case -504: r = "STATUS_TIMEOUT"; 
	    	break;
	    	case -505: r = "STATUS_USB_CONFIGURE"; 
	    	break;
	    	case -506: r = "STATUS_REQUEST_LF"; 
	    	break;
	
	    	case -900: r = "STATUS_ESTOP"; 
	    	break;
	    	case -901: r = "STATUS_SERIAL_CANCEL"; 
	    	break;
	    	case -902: r = "STATUS_TRAVEL_MIN"; 
	    	break;
	    	case -903: r = "STATUS_TRAVEL_MAX"; 
	    	break;
	    	case -904: r = "STATUS_LIMIT_MIN"; 
	    	break;
	    	case -905: r = "STATUS_LIMIT_MAX"; 
	    	break;
	    	case -906: r = "STATUS_PROBE_FAILED"; 
	    	break;
	
	    	case -1000: r = "STATUS_LINUX"; 
	    	break;
	    	case -1001: r = "STATUS_LINUX_EPERM"; 
	    	break;
	    	case -1004: r = "STATUS_LINUX_EINTR"; 
	    	break;
	    	case -1005: r = "STATUS_LINUX_EIO"; 
	    	break;
	    	case -1011: r = "STATUS_LINUX_EAGAIN"; 
	    	break;
	    	case -1013: r = "STATUS_LINUX_EACCES"; 
	    	break;
	    	case -1022: r = "STATUS_LINUX_EINVAL"; 
	    	break;

    		default: r = "UNRECOGNIZED_ERROR";
    		break;
    	}
    	return r;
    }

    @Override
    public Wizard getConfigurationWizard() {
        // TODO Auto-generated method stub
        return new AbstractEthernetDriverConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }
}

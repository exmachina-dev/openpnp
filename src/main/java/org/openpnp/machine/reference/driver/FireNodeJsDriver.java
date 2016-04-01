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

package org.openpnp.machine.reference.driver;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

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
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.BaseRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.MultipartBody;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * TODO: Consider adding some type of heartbeat to the firmware. TODO: The whole movement wait lock
 * thing has to go. See if we can do a P4 type command like the other drivers to wait for movement
 * to complete. Disabled axes don't send status reports, so movement wait lock never happens.
 * Probably short moves also won't.
 */
public class FireNodeJsDriver extends AbstractEthernetDriver implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(FireNodeJsDriver.class);
    private static final int minimumRequiredVersion = 4; // Version is major * 1000 + minor. Patch number is no checked.

    @Attribute(required = false)
    private double feedRateMmPerMinute = 5000;
    @Element(required = false)
    private Location homeLocation = new Location(LengthUnit.Millimeters);

    private double x, y, z, c;
    private Thread readerThread;
    private boolean disconnectRequested;
    private Object commandLock = new Object();
    private Object movementWaitLock = new Object();
    private HttpResponse<JsonNode> lastResponse;
    private HttpResponse<JsonNode> baseResponse;
    private boolean connected;
    private int connectedVersion;

    public FireNodeJsDriver() {}

    @Override
    public synchronized void connect() throws Exception {
        super.connect();

        Unirest.setDefaultHeader("Accept", "application/json");
        this.setHostUrl(this.protocol, this.host, this.port);
        
        for (int i = 0; i < 1 && !connected; i++) {
            try {
                HttpResponse<JsonNode> helloResponse = sendCommand("/firenodejs/models");
                if (helloResponse.getStatus() != 200) {
                	throw new Exception(String.format("Unable to connect to {} ({})", hostUrl, helloResponse.getStatus()));
                } else {
                	logger.debug(String.format("{} said: {}", hostUrl, helloResponse.getBody()));
                }

                HttpResponse<JsonNode> response = sendCommand("/firenodejs/models");
                JSONObject v = response.getBody().getObject().getJSONObject("version");
                connectedVersion = v.getInt("major") * 1000 + v.getInt("minor");
                String versionString = v.getString("major") + "." + v.getString("minor") + "." + v.getString("patch");
                logger.debug("Version: {}", versionString);
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

        // TODO: iod28 switches pin 28 which is the power supply pin on FPD from TW
        // Should be configurable
        sendCommand(Unirest.post("/firestep").field("iod28", enabled ? "1" : "0"));
        
        // TODO: iod5 switches pin 5 which is the end effector led ring on FPD from TW
        // Should be configurable
        sendCommand(Unirest.post("/firestep").field("iod5", enabled ? "1" : "0"));
    }

    @Override
    public void home(ReferenceHead head) throws Exception {
        synchronized (movementWaitLock) {
            HttpResponse<JsonNode> response = sendCommand("/firestep", new JSONArray().put("hom"));
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
        sendCommand("/firestep", new JSONArray().put("mov").put(homeCoords));
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
        HttpResponse<JsonNode> response = sendCommand("/firestep", new JSONArray().put("mov").put(newCoords));

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
        	JSONArray actuatorField = new JSONArray();
        	actuatorField.put(String.format("iod{}", actuator.getIndex()));
        	actuatorField.put(on ? 1 : 0);
            sendCommand("/firestep", actuatorField);
        }
    }

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        return new Location(LengthUnit.Millimeters, x, y, z, c).add(hm.getHeadOffsets());
    }

    private int getResponseStatusCode(HttpResponse<JsonNode> o) {
        return o.getStatus();
    }

    public synchronized void disconnect() {
        disconnectRequested = true;
        connected = false;

        try {
            if (readerThread != null && readerThread.isAlive()) {
                readerThread.join();
            }
        }
        catch (Exception e) {
            logger.error("disconnect()", e);
        }

        try {
            super.disconnect();
        }
        catch (Exception e) {
            logger.error("disconnect()", e);
        }
        disconnectRequested = false;
    }

    public synchronized HttpResponse<JsonNode> sendCommand(String command) throws Exception {
    	return sendCommand(Unirest.get(hostUrl + command));
    }
    
    public synchronized HttpResponse<JsonNode> sendCommand(String command, JSONArray field) throws Exception {
    	HttpRequestWithBody jsonCommand = Unirest.post(hostUrl + command);
    	if (field.length() > 1) {
    		jsonCommand.field(field.getString(0), field.get(1));
    	} else {
    		jsonCommand.field(field.getString(0), "");
    	}
    	
    	return sendCommand(jsonCommand);
    }
    
    public synchronized HttpResponse<JsonNode> sendCommand(String command, Collection<JSONArray> fields) throws Exception {
    	HttpRequestWithBody jsonCommand = Unirest.post(hostUrl + command);
    	for (JSONArray field: fields) {
    		jsonCommand.field(field.getString(0), field.get(1));
    	}
    	
    	return sendCommand(jsonCommand);
    }
    /*
    public synchronized HttpResponse<JsonNode> sendCommand(GetRequest command) throws Exception {
    	return sendCommand(command);
    }
    
    public synchronized HttpResponse<JsonNode> sendCommand(MultipartBody command) throws Exception {
    	return sendCommand(command);
    }*/

    public synchronized HttpResponse<JsonNode> sendCommand(BaseRequest command) throws Exception {

    	HttpResponse<JsonNode> response;
        synchronized (commandLock) {
            logger.debug("sendCommand({})", command.getHttpRequest().getUrl());
            response = command.asJson();
            if (response.getStatus() == 200) {
            	logger.debug("success");
            }
        }
        if (response.getHeaders().size() == 0) {
            throw new Exception("Command did not return a response");
        }

        if (response.getStatus() != 200) {
            throw new Exception("Request failed. Status code: " + response.getStatus());
        }
        return response;
    }

    public void run() {
        while (!disconnectRequested) {
            String line;
            line = "test";
            logger.trace(line);
        }
    }

    private void processStatusReport(JSONObject o) {
        if (o.has("stat")) {
            int stat = o.getInt("stat");
            if (stat == 3) {
                synchronized (movementWaitLock) {
                    movementWaitLock.notifyAll();
                }
            }
        }
    }

    // TODO: If no movement is happening this will never return. We may want to
    // have it issue a status report request now and then so it doesn't sit
    // forever.
    private void waitForMovementComplete() throws Exception {
        synchronized (movementWaitLock) {
            movementWaitLock.wait();
        }
    }

    private void getStatusCodeDetails(int statusCode) {
        // 0 | TG_OK | universal OK code (function completed successfully)
        // 1 | TG_ERROR | generic error return (EPERM)
        // 2 | TG_EAGAIN | function would block here (call again)
        // 3 | TG_NOOP | function had no-operation
        // 4 | TG_COMPLETE | operation is complete
        // 5 | TG_TERMINATE | operation terminated (gracefully)
        // 6 | TG_RESET | operation was hard reset (sig kill)
        // 7 | TG_EOL | function returned end-of-line or end-of-message
        // 8 | TG_EOF | function returned end-of-file
        // 9 | TG_FILE_NOT_OPEN
        // 10 | TG_FILE_SIZE_EXCEEDED
        // 11 | TG_NO_SUCH_DEVICE
        // 12 | TG_BUFFER_EMPTY
        // 13 | TG_BUFFER_FULL
        // 14 | TG_BUFFER_FULL_FATAL
        // 15 | TG_INITIALIZING | initializing - not ready for use
        // 16-19 | TG_ERROR_16 - TG_ERROR_19 | reserved
        // 20 | TG_INTERNAL_ERROR | unrecoverable internal error
        // 21 | TG_INTERNAL_RANGE_ERROR | number range error other than by user
        // input
        // 22 | TG_FLOATING_POINT_ERROR | number conversion error
        // 23 | TG_DIVIDE_BY_ZERO
        // 24 | TG_INVALID_ADDRESS
        // 25 | TG_READ_ONLY_ADDRESS
        // 26 | TG_INIT_FAIL | Initialization failure
        // 27 | TG_SHUTDOWN | System shutdown occurred
        // 28 | TG_MEMORY_CORRUPTION | Memory corruption detected
        // 29-39 | TG_ERROR_26 - TG_ERROR_39 | reserved
        // 40 | TG_UNRECOGNIZED_COMMAND | parser didn't recognize the command
        // 41 | TG_EXPECTED_COMMAND_LETTER | malformed line to parser
        // 42 | TG_BAD_NUMBER_FORMAT | number format error
        // 43 | TG_INPUT_EXCEEDS_MAX_LENGTH | input string is too long
        // 44 | TG_INPUT_VALUE_TOO_SMALL | value is under minimum for this
        // parameter
        // 45 | TG_INPUT_VALUE_TOO_LARGE | value is over maximum for this
        // parameter
        // 46 | TG_INPUT_VALUE_RANGE_ERROR | input error: value is out-of-range
        // for this parameter
        // 47 | TG_INPUT_VALUE_UNSUPPORTED | input error: value is not supported
        // for this parameter
        // 48 | TG_JSON_SYNTAX_ERROR | JSON string is not well formed
        // 49 | TG_JSON_TOO_MANY_PAIRS | JSON string or has too many name:value
        // pairs
        // 50 | TG_JSON_TOO_LONG | JSON output string too long for output buffer
        // 51 | TG_NO_BUFFER_SPACE | Buffer pool is full and cannot perform this
        // operation
        // 52 - 59 | TG_ERROR_51 - TG_ERROR_59 | reserved
        // 60 | TG_ZERO_LENGTH_MOVE | move is zero length
        // 61 | TG_GCODE_BLOCK_SKIPPED | block was skipped - usually because it
        // was is too short
        // 62 | TG_GCODE_INPUT_ERROR | general error for gcode input
        // 63 | TG_GCODE_FEEDRATE_ERROR | no feedrate specified
        // 64 | TG_GCODE_AXIS_WORD_MISSING | command requires at least one axis
        // present
        // 65 | TG_MODAL_GROUP_VIOLATION | gcode modal group error
        // 66 | TG_HOMING_CYCLE_FAILED | homing cycle did not complete
        // 67 | TG_MAX_TRAVEL_EXCEEDED
        // 68 | TG_MAX_SPINDLE_SPEED_EXCEEDED
        // 69 | TG_ARC_SPECIFICATION_ERROR | arc specification error
        // 70-79 | TG_ERROR_70 - TG_ERROR_79 | reserved
        // 80-99 | Expansion | Expansion ranges
        // 100-119 | Expansion |
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

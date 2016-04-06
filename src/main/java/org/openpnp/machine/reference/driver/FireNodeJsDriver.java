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

import javax.swing.Action;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.driver.wizards.FireNodeJsDriverConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.BaseRequest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;

public class FireNodeJsDriver extends AbstractEthernetDriver {
    private static final Logger logger = LoggerFactory.getLogger(FireNodeJsDriver.class);
    private static final int minimumRequiredVersion = 20; // Version is major * 1000 + minor. Patch number is no checked.

    @Attribute(required = false)
    private double feedRateMmPerMinute = 5000;
    @Element(required = false)
    private Location homeLocation = new Location(LengthUnit.Millimeters);

    @Attribute(required = false)
    protected LengthUnit units = LengthUnit.Millimeters;

    private double x, y, z, c;
    private Object commandLock = new Object();
    private Object movementWaitLock = new Object();
    private boolean connected;
    private int connectedVersion;

    // Invert
    @Element(required = false)
	protected boolean invertMotorX;
    @Element(required = false)
    protected boolean invertMotorY;
    @Element(required = false)
    protected boolean invertMotorZ;
    @Element(required = false)
    protected boolean invertAxisX;
    @Element(required = false)
    protected boolean invertAxisY;
    // LPP
    @Element(required = false)
    protected boolean disableLpp;
    @Element(required = false)
    protected boolean disableLppForShortMoves;
    // Power supply
    @Element(required = false)
    protected boolean powerSupplyManagement;
    @Element(required = false)
    protected int powerSupplyPin = -1;
    // Vacuum
    @Element(required = false)
    protected int vacuumPin = -1;
    @Element(required = false)
    protected boolean invertVacuumPin;
    // Led rings
    @Element(required = false)
    protected int endEffectorLedRingPin = -1;
    @Element(required = false)
    protected int upLookingLedRingPin = -1;
    // Advanced config
    @Element(required = false)
    protected String beforeResetConfig = "";

    public FireNodeJsDriver() {}

    @Override
    public synchronized void connect() throws Exception {
        super.connect();
        
        connected = false;
        String versionString = null;
        
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
                versionString = String.format("%d.%d.%d", v.getInt("major"), v.getInt("minor"), v.getInt("patch"));
                logger.debug("Firenodejs version: {}", versionString);
                connected = true;
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
        	logger.debug(String.format("%d < %d", connectedVersion, minimumRequiredVersion));
            throw new Exception(String.format(
                    "This driver requires FireNodeJs version %d.%d or higher. You are running version %.2f",
                    minimumRequiredVersion / 1000, minimumRequiredVersion - (Math.round(minimumRequiredVersion/1000)),
                    connectedVersion));
        } else {
        	logger.debug(String.format("Version check successful: %.2f", connectedVersion/1));
        }

        logger.debug(String.format("Connected to FireNodeJs Version: {}", versionString));
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        if (enabled && !connected) {
            connect();
        }

        if (connected) {

            if (enabled) {
                enablePowerSupply(enabled); // Power supply should be enabled before issuing a reset, otherwise machine cannot home
                sendMotorConfig();
                reset();
            } else {
                reset();
                enablePowerSupply(enabled);
            }

            enableEndEffectorLedRing(enabled);
            enableUpLookingLedRing(enabled);
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

        Location homeLocation = this.homeLocation.convertToUnits(units);
        moveTo(homeLocation, feedRateMmPerMinute);
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed)
            throws Exception {
        location = location.subtract(hm.getHeadOffsets());

        location = location.convertToUnits(units);

        moveTo(location, speed);
    }

    public void moveTo(double x, double y, double z, double c, double speed) throws Exception {
        moveTo(new Location(units, x, y, z, c), speed);
    }

    public void moveTo(Location newLocation, double speed) throws Exception {

        if (Math.abs(newLocation.getRotation() - c) >= 0.01) {
            int rotSteps = (int) (newLocation.getRotation() * 3200 / 360);
            JSONObject newRotation = new JSONObject();
            newRotation.put("mova", rotSteps);
            HttpResponse<JsonNode> response = sendCommand("/firestep", newRotation);
            checkResponseCode(response);
        }

        Location currentLocation = new Location(units, this.x, this.y, this.z, this.c);
        if (currentLocation.getLinearDistanceTo(newLocation) != 0 || this.z != newLocation.getZ())
        {
            JSONObject newCoords = new JSONObject();
            if (!Double.isNaN(newLocation.getX())) {
                newCoords.put("x", !invertAxisX ? newLocation.getX() : -newLocation.getX());
            }
            if (!Double.isNaN(newLocation.getY())) {
                newCoords.put("y", !invertAxisY ? newLocation.getY() : -newLocation.getY());
            }
            if (!Double.isNaN(newLocation.getZ())) {
                newCoords.put("z", newLocation.getZ());
            }

            if (disableLpp) {
                if (!disableLppForShortMoves && currentLocation.getLinearDistanceTo(newLocation) > 100) {
                    newCoords.put("lpp", true);
                } else {
                    newCoords.put("lpp", false);
                }
            }

            HttpResponse<JsonNode> response = sendCommand("/firestep", new JSONObject().put("mov", newCoords));
            checkResponseCode(response);
        }

        if (!Double.isNaN(newLocation.getX())) {
            this.x = newLocation.getX();
        }
        if (!Double.isNaN(newLocation.getY())) {
            this.y = newLocation.getY();
        }
        if (!Double.isNaN(newLocation.getZ())) {
            this.z = newLocation.getZ();
        }
        if (!Double.isNaN(newLocation.getRotation())) {
            this.c = newLocation.getRotation();
        }
    }


    @Override
    public void pick(ReferenceNozzle nozzle) throws Exception {
        enableVacuumPin(true);
    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {
        enableVacuumPin(false);
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
        if (actuator.getIndex() > 0) {
            actuate(actuator.getIndex(), on);
        }
    }

    public void actuate(int index, boolean on) throws Exception {
        if (index > 0) {
            sendCommand("/firestep", new JSONObject().put(String.format("iod%d", index), on));
        }
    }

    private  void enablePowerSupply(boolean on) throws Exception {
        if (powerSupplyManagement) {
            actuate(powerSupplyPin, on);
        }
    }

    private  void enableEndEffectorLedRing(boolean on) throws Exception {
        actuate(endEffectorLedRingPin, on);
    }

    private  void enableUpLookingLedRing(boolean on) throws Exception {
        actuate(upLookingLedRingPin, on);
    }

    private  void enableVacuumPin(boolean on) throws Exception {
        if (invertVacuumPin) {
            actuate(vacuumPin, !on);
        } else {
            actuate(vacuumPin, on);
        }
    }

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        Location location = new Location(units, x, y, z, c).add(hm.getHeadOffsets());
        if (!(hm instanceof Nozzle)) {
            location = location.derive(null, null, 0d, null);
        }
        return location;
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

    private synchronized  void sendMotorConfig() throws Exception {
        JSONObject motorConfig = new JSONObject();
        motorConfig.put("xdh", !invertMotorX);
        motorConfig.put("ydh", !invertMotorY);
        motorConfig.put("zdh", !invertMotorZ);
        sendCommand("/firestep", motorConfig);
    }
    private synchronized  void reset() throws Exception {
        if (beforeResetConfig != "") {
            JsonNode beforeResetObject = new JsonNode(beforeResetConfig);
            if (beforeResetObject.isArray())
                sendCommand("/firestep/reset", beforeResetObject.getArray());
            else
                sendCommand("/firestep/reset", beforeResetObject.getObject());
        } else {
            sendCommand("/firestep/reset", "");
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
    	return sendCommand(command, new JSONObject().put(field, ""));
    }

    public synchronized HttpResponse<JsonNode> sendCommand(String command, JSONObject object) throws Exception {
    	return sendCommand(command, new JSONArray().put(object));
    }

    public synchronized HttpResponse<JsonNode> sendCommand(String command, JSONArray jsonArray) throws Exception {
    	HttpRequestWithBody jsonCommand = Unirest.post(hostUrl + command);

    	return sendCommand(jsonCommand.body(new JsonNode(jsonArray.toString())));
    }

    public synchronized HttpResponse<JsonNode> sendCommand(BaseRequest command) throws Exception {
    	
    	if (hostUrl == null) {
    		logger.error("Host url is not defined");
    		throw new Exception("Host url is not defined");
    	}

    	HttpResponse<JsonNode> response;
        synchronized (commandLock) {
            logger.debug("sendCommand({})", command.getHttpRequest().getUrl());
            logger.debug("{}", command.getHttpRequest().getHeaders());
            try {
            	logger.debug("{}", command.getHttpRequest().getBody().getEntity().getContent().toString());
            } catch (NullPointerException e) { }
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
        return new FireNodeJsDriverConfigurationWizard(this);
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

    public boolean getInvertMotorX() { return invertMotorX; }

    public boolean getInvertMotorY() { return invertMotorY; }

    public boolean getInvertMotorZ() { return invertMotorZ; }

    public boolean getInvertAxisX() { return invertAxisX; }

    public boolean getInvertAxisY() { return invertAxisY; }

    public boolean getDisableLpp() { return disableLpp; }

    public boolean getDisableLppForShortMoves() { return disableLppForShortMoves; }

    public boolean getPowerSupplyManagement() { return powerSupplyManagement; }

    public int getPowerSupplyPin() { return powerSupplyPin; }

    public int getVacuumPin() { return vacuumPin; }

    public boolean getInvertVacuumPin() { return invertVacuumPin; }

    public int getEndEffectorLedRingPin() { return endEffectorLedRingPin; }

    public int getUpLookingLedRingPin() { return upLookingLedRingPin; }

    public String getBeforeResetConfig() { return beforeResetConfig; }

    public void setInvertMotorX(boolean invertMotorX) { this.invertMotorX = invertMotorX; }

    public void setInvertMotorY(boolean invertMotorY) { this.invertMotorY = invertMotorY; }

    public void setInvertMotorZ(boolean invertMotorZ) { this.invertMotorZ = invertMotorZ; }

    public void setInvertAxisX(boolean invertAxisX) { this.invertAxisX = invertAxisX; }

    public void setInvertAxisY(boolean invertAxisY) { this.invertAxisY = invertAxisY; }

    public void setDisableLpp(boolean disable) { this.disableLpp = disable; }

    public void setDisableLppForShortMoves(boolean disable) { this.disableLppForShortMoves = disable; }

    public void setPowerSupplyManagement(boolean powerSupplyManagement) { this.powerSupplyManagement = powerSupplyManagement; }

    public void setPowerSupplyPin(int powerSupplyPin) { this.powerSupplyPin = powerSupplyPin; }

    public void setVacuumPin(int vacuumPin) { this.vacuumPin = vacuumPin; }

    public void setInvertVacuumPin(boolean invertVacuumPin) { this.invertVacuumPin = invertVacuumPin; }

    public void setEndEffectorLedRingPin(int endEffectorLedRingPin) { this.endEffectorLedRingPin = endEffectorLedRingPin; }

    public void setUpLookingLedRingPin(int upLookingLedRingPin) { this.upLookingLedRingPin = upLookingLedRingPin; }

    public void setBeforeResetConfig(String beforeResetConfig) { this.beforeResetConfig = beforeResetConfig; }
}

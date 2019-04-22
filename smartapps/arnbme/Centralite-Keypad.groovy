/**
 *  Centralite Keypad
 *
 *  Copyright 2015-2016 Mitch Pond, Zack Cornelius
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Apr 22, 2019 changed battery back to % from volts for users. Temperature is still very wrong 
 *  Mar 31, 2019 routine disarm and others issued multiple times. fixed in other modules 
 *  Mar 31, 2019 routine disarm threw an error caused by HE sending a delay value, add delay parm that is ignored
 *  Mar 31, 2019 deprecate Sep 20, 2018 change, HE should be fast enough for proper acknowledgements
 *  Feb 26, 2019 in sendRawStatus set seconds to Integer or it fails
 *	Feb 26, 2019 HE device.currentValue gives value at start of event, use true as second parameter to get live actual value
 *  Feb 25, 2019 kill setmodehelper on detentrydelay and setexitdelay commands, mode help sets mode icon lights
 *					error found in Hubitat with entry delay
 *  Feb 22, 2019 convertToHexString default in ST not available in HE, change width to 2
 *  Feb 21, 2019 V1.0.1 Hubitat command names vary from Smartthings, add additional commands
 *  Sep 20, 2018 per ST tech support. Issue acknowlegement in HandleArmRequest
 *               disable routines: acknowledgeArmRequest and sendInvalidKeycodeResponse allowing SHM Delay to have no code changes
 *
 *  Sep 18, 2018 comment out health check in an attempt to fix timout issue  (no improvement) 
 *  Sep 04, 2018 add health check and vid for new phone app. 
 *  Mar 25, 2018 add volts to battery message 		
 *  Aug 25, 2017 deprecate change of Jul 12, 2017, change from Jul 25 & 26, 2017 remains but is no longer needed or used 		
 *  Jul 26, 2017 Stop entryDelay from updating field lastUpdate or alarm is not triggered in CoRE
 *			pistons that assume a time change means alarm mode(off or on) was reset
 *  Jul 25, 2017 in formatLocalTime add seconds to field lastUpdate. 
 * 			need seconds to catch a rearm within the open time delay in Core Front Door Opens piston
 * 			otherwise alarm sounds after rearm
 *  Jul 12, 2017 in sendStatustoDevice light Night button not HomeStay button (no such mode in SmartHome) 		
 */
metadata {
	definition (name: "Centralitex Keypad", namespace: "mitchpond", author: "Mitch Pond", vid: "generic-motion") {
		capability "SecurityKeypad"
		capability "Battery"
		capability "Configuration"
        capability "Motion Sensor"
		capability "Sensor"
		capability "Temperature Measurement"
		capability "Refresh"
		capability "Lock Codes"
		capability "Tamper Alert"
		capability "Tone"
//		capability "button"
//      capability "polling"
//      capability "Contact Sensor"
		
		attribute "armMode", "String"
        attribute "lastUpdate", "String"
		
		command "setDisarmed"
		command "setArmedAway"
		command "setArmedStay"
		command "setArmedNight"
		command "setExitDelay", ['number']
		command "setEntryDelay", ['number']
		command "testCmd"
		command "sendInvalidKeycodeResponse"
		command "acknowledgeArmRequest",['number']
//		Hubitat Commands V1.0.1
		command "disarm"
		command "armAway"
		command "armHome"
		command "armNight"
		command "entry", ['number']
		
		fingerprint endpointId: "01", profileId: "0104", deviceId: "0401", inClusters: "0000,0001,0003,0020,0402,0500,0B05", outClusters: "0019,0501", manufacturer: "CentraLite", model: "3400", deviceJoinName: "Xfinity 3400-X Keypad"
		fingerprint endpointId: "01", profileId: "0104", deviceId: "0401", inClusters: "0000,0001,0003,0020,0402,0500,0501,0B05,FC04", outClusters: "0019,0501", manufacturer: "CentraLite", model: "3405-L", deviceJoinName: "Iris 3405-L Keypad"
	}
	
	preferences{
		input ("tempOffset", "number", title: "Enter an offset to adjust the reported temperature",
				defaultValue: 0, displayDuringSetup: false)
		input ("beepLength", "number", title: "Enter length of beep in seconds",
				defaultValue: 1, displayDuringSetup: false)
                
        input ("motionTime", "number", title: "Time in seconds for Motion to become Inactive (Default:10, 0=disabled)",	defaultValue: 10, displayDuringSetup: false)
        input ("logdebugs", "bool", title: "Log debugging messages", defaultValue: false, displayDuringSetup: false)
        input ("logtraces", "bool", title: "Log trace messages", defaultValue: false, displayDuringSetup: false)
	}

}

// parse events into attributes
def parse(String description) {
	logdebug "Parsing '${description}'";
	def results = [];
	
	//------Miscellaneous Zigbee message------//
	if (description?.startsWith('catchall:')) {

		//logdebug zigbee.parse(description);

		def message = zigbee.parse(description);
		
		//------Profile-wide command (rattr responses, errors, etc.)------//
		if (message?.isClusterSpecific == false) {
			//------Default response------//
			if (message?.command == 0x0B) {
				if (message?.data[1] == 0x81) 
					log.error "Device: unrecognized command: "+description;
				else if (message?.data[1] == 0x80) 
					log.error "Device: malformed command: "+description;
			}
			//------Read attributes responses------//
			else if (message?.command == 0x01) {
				if (message?.clusterId == 0x0402) {
					logdebug "Device: read attribute response: "+description;

					results = parseTempAttributeMsg(message)
				}}
			else 
				log.warn "Unhandled profile-wide command: "+description;
		}
		//------Cluster specific commands------//
		else if (message?.isClusterSpecific) {
			//------IAS ACE------//
			if (message?.clusterId == 0x0501) {
				if (message?.command == 0x07) {
                	motionON()
				}
                else if (message?.command == 0x04) {
                	results = createEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "$device.displayName panic button was pushed", isStateChange: true)
                    panicContact()
                }
				else if (message?.command == 0x00) {
					results = handleArmRequest(message)
					logtrace results
				}
			}
			else log.warn "Unhandled cluster-specific command: "+description
		}
	}
	//------IAS Zone Enroll request------//
	else if (description?.startsWith('enroll request')) {
		logtrace "Sending IAS enroll response..."
		results = zigbee.enrollResponse()
	}
	//------Read Attribute response------//
	else if (description?.startsWith('read attr -')) {
		results = parseReportAttributeMessage(description)
	}
	//------Temperature Report------//
	else if (description?.startsWith('temperature: ')) {
		logdebug "Got ST-style temperature report.."
		results = createEvent(getTemperatureResult(zigbee.parseHATemperatureValue(description, "temperature: ", getTemperatureScale())))
		logdebug results
	}
    else if (description?.startsWith('zone status ')) {
    	results = parseIasMessage(description)
    }
	return results
}


def configure() {
    logtrace "--- Configure Called"
    String hubZigbeeId = swapEndianHex(device.hub.zigbeeEui)
    def cmd = [
        //------IAS Zone/CIE setup------//
        "zcl global write 0x500 0x10 0xf0 {${hubZigbeeId}}", "delay 100",
        "send 0x${device.deviceNetworkId} 1 1", "delay 200",

        //------Set up binding------//
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x500 {${device.zigbeeId}} {}", "delay 200",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x501 {${device.zigbeeId}} {}", "delay 200",
        
    ] + 
    zigbee.configureReporting(1,0x20,0x20,3600,43200,0x01) + 
    zigbee.configureReporting(0x0402,0x00,0x29,30,3600,0x0064)

    return cmd + refresh()
}

def poll() { 
	refresh()
}

def refresh() {
	 return sendStatusToDevice() +
		zigbee.readAttribute(0x0001,0x20) + 
		zigbee.readAttribute(0x0402,0x00)
}

private formatLocalTime(time, format = "EEE, MMM d yyyy @ h:mm:ss.SSS a z") {
	if (time instanceof Long) {
    	time = new Date(time)
    }
	if (time instanceof String) {
    	//get UTC time
    	time = timeToday(time, location.timeZone)
    }   
    if (!(time instanceof Date)) {
    	return null
    }
	def formatter = new java.text.SimpleDateFormat(format)
	formatter.setTimeZone(location.timeZone)
	return formatter.format(time)
}

private parseReportAttributeMessage(String description) {
	Map descMap = (description - "read attr - ").split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
	//logdebug "Desc Map: $descMap"

	def results = []
	
	if (descMap.cluster == "0001" && descMap.attrId == "0020") {
		logdebug "Received battery level report"
//		sendNotificationEvent ("Received battery level report descMap.value")
		results = createEvent(getBatteryResult(Integer.parseInt(descMap.value, 16)))
	}
    else if (descMap.cluster == "0001" && descMap.attrId == "0034")
    {
    	logdebug "Received Battery Rated Voltage: ${descMap.value}"
//		sendNotificationEvent ("Received Battery Rated Voltage: descMap.value")
    }
    else if (descMap.cluster == "0001" && descMap.attrId == "0036")
    {
    	logdebug "Received Battery Alarm Voltage: ${descMap.value}"
//		sendNotificationEvent ("Received Battery Alarm Voltage: descMap.value")
    }
	else if (descMap.cluster == "0402" && descMap.attrId == "0000") {
		def value = getTemperature(descMap.value)
		results = createEvent(getTemperatureResult(value))
	}

	return results
}

private parseTempAttributeMsg(message) {
	byte[] temp = message.data[-2..-1].reverse()
	createEvent(getTemperatureResult(getTemperature(temp.encodeHex() as String)))
}

private Map parseIasMessage(String description) {
    List parsedMsg = description.split(' ')
    String msgCode = parsedMsg[2]
    
    Map resultMap = [:]
    switch(msgCode) {
        case '0x0020': // Closed/No Motion/Dry
        	resultMap = getContactResult('closed')
            break

        case '0x0021': // Open/Motion/Wet
        	resultMap = getContactResult('open')
            break

        case '0x0022': // Tamper Alarm
            break

        case '0x0023': // Battery Alarm
            break

        case '0x0024': // Supervision Report
        	resultMap = getContactResult('closed')
            break

        case '0x0025': // Restore Report
        	resultMap = getContactResult('open')
            break

        case '0x0026': // Trouble/Failure
            break

        case '0x0028': // Test Mode
            break
        case '0x0000':
			resultMap = createEvent(name: "tamper", value: "clear", isStateChange: true, displayed: false)
            break
        case '0x0004':
			resultMap = createEvent(name: "tamper", value: "detected", isStateChange: true, displayed: false)
            break;
        default:
        	log.warn "Invalid message code in IAS message: ${msgCode}"
    }
    return resultMap
}


private Map getMotionResult(value) {
	String linkText = getLinkText(device)
	String descriptionText = value == 'active' ? "${linkText} detected motion" : "${linkText} motion has stopped"
	return [
		name: 'motion',
		value: value,
		descriptionText: descriptionText
	]
}
def motionON() {
//    logdebug "--- Motion Detected"
    sendEvent(name: "motion", value: "active", displayed:true, isStateChange: true)
    
	//-- Calculate Inactive timeout value
	def motionTimeRun = (settings.motionTime?:0).toInteger()

	//-- If Inactive timeout was configured
	if (motionTimeRun > 0) {
//		logdebug "--- Will become inactive in $motionTimeRun seconds"
		runIn(motionTimeRun, "motionOFF")
	}
}

def motionOFF() {
//	logdebug "--- Motion Inactive (OFF)"
    sendEvent(name: "motion", value: "inactive", displayed:true, isStateChange: true)
}

def panicContact() {
	logdebug "--- Panic button hit"
    sendEvent(name: "contact", value: "open", displayed: true, isStateChange: true)
    runIn(3, "panicContactClose")
}

def panicContactClose()
{
	sendEvent(name: "contact", value: "closed", displayed: true, isStateChange: true)
}

//TODO: find actual good battery voltage range and update this method with proper values for min/max
//
//Converts the battery level response into a percentage to display in ST
//and creates appropriate message for given level

private getBatteryResult(rawValue) {
	def linkText = getLinkText(device)

	def result = [name: 'battery']

	def volts = rawValue / 10
	def descriptionText=""
	if (volts > 3.5) {
		result.descriptionText = "${linkText} battery has too much power (${volts} volts)."
	}
	else {
		def minVolts = 2.5
		def maxVolts = 3.0
		def pct = (volts - minVolts) / (maxVolts - minVolts)
//		result.value = Math.min(100, (int) pct * 100)
		result.value = Math.min(100, Math.round(pct * 100))
		descriptionText = "${linkText} battery was ${result.value}% $volts volts"
		result.descriptionText = descriptionText
		logdebug "$result"
//      result.value=rawValue
		}
//	sendNotificationEvent "${result.descriptionText}"
//	sendNotificationEvent (descriptionText)
	return result
}

private getTemperature(value) {
	def celcius = Integer.parseInt(value, 16).shortValue() / 100
//	log.debug "Celcius: $celcius Farenheit: ${celsiusToFahrenheit(celcius) as Integer}"
	if(getTemperatureScale() == "C"){  
		return celcius
	} else {
		return celsiusToFahrenheit(celcius) as Integer
	}
}

private Map getTemperatureResult(value) {
	logdebug 'TEMP'
	def linkText = getLinkText(device)
	if (tempOffset) {
		def offset = tempOffset as int
		def v = value as int
		value = v + offset
	}
	def descriptionText = "${linkText} was ${value}Ã‚Â°${temperatureScale}"
	return [
		name: 'temperature',
		value: value,
		descriptionText: descriptionText
	]
}

//------Command handlers------//
private handleArmRequest(message){
	def keycode = new String(message.data[2..-2] as byte[],'UTF-8')
	def reqArmMode = message.data[0]
	//state.lastKeycode = keycode
	logdebug "Received arm command with keycode/armMode: ${keycode}/${reqArmMode}"

	//Acknowledge the command. This may not be *technically* correct, but it works
	/*List cmds = [
				 "raw 0x501 {09 01 00 0${reqArmMode}}", "delay 200",
				 "send 0x${device.deviceNetworkId} 1 1", "delay 500"
				]
	def results = cmds?.collect{ new hubitat.device.HubAction(it,, hubitat.device.Protocol.ZIGBEE) } + createCodeEntryEvent(keycode, reqArmMode)
	*/
//	def results = createCodeEntryEvent(keycode, reqArmMode)
//	List cmds = [
//				 "raw 0x501 {09 01 00 0${reqArmMode}}",
//				 "send 0x${device.deviceNetworkId} 1 1", "delay 100"
//				]
//	def results = cmds?.collect{ new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE) } + createCodeEntryEvent(keycode, reqArmMode)     
//	log.trace "Method: handleArmRequest(message): "+results
//	return results
//	cmds
	createCodeEntryEvent(keycode, reqArmMode)
}

def createCodeEntryEvent(keycode, armMode) {
	createEvent(name: "codeEntered", value: keycode as String, data: armMode as String, 
				isStateChange: true, displayed: false)
}

//
//The keypad seems to be expecting responses that are not in-line with the HA 1.2 spec. Maybe HA 1.3 or Zigbee 3.0??
//
private sendStatusToDevice(armModex='') {
	logdebug 'Entering sendStatusToDevice armModex: '+armModex+', Device.armMode: '+device.currentValue('armMode',true)  	
	def armMode=null
	if (armModex=='')
		{
//		logdebug "using device armMode"
		armMode = device.currentValue("armMode",true)
		}
	else
		{
//		logdebug "using passed armModex"
		armMode = armModex
		}
	def status = ''
	if (armMode == null || armMode == 'disarmed') status = 0
	else if (armMode == 'armedAway') status = 3
	else if (armMode == 'armedStay') status = 1	
	else if (armMode == 'armedNight') status = 2
	else logdebug 'Invalid Arm Mode in sendStatusToDevice: '+armMode
	
	// If we're not in one of the 4 basic modes, don't update the status, don't want to override beep timings, exit delay is dependent on it being correct
	if (status != '')
	{
		return sendRawStatus(status)
	}
    else
    {
    	return []
    }
}


// Statuses:
// 00 - Disarmed
// 01 - Armed Stay
// 02 - Armed Night
// 03 - Armed Away
// 04 - ?
// 05 - Fast beep (1 per second)
// 05 - Entry delay (Uses seconds) Appears to keep the status lights as it was
// 06 - Amber status blink (Ignores seconds)
// 07 - ?
// 08 - Red status blink  //lights home stay button
// 09 - ?
// 10 - Exit delay Slow beep (2 per second, accelerating to 1 beep per second for the last 10 seconds) - With red flashing status - Uses seconds
// 11 - ?
// 12 - ?
// 13 - ?

private sendRawStatus(status, secs = 00) {
	def seconds=secs as Integer
	logdebug "sendRawStatus info ${zigbee.convertToHexString(status,2)}${zigbee.convertToHexString(seconds,2)} to device..."

    
    // Seems to require frame control 9, which indicates a "Server to client" cluster specific command (which seems backward? I thought the keypad was the server)
    List cmds = ["raw 0x501 {09 01 04 ${zigbee.convertToHexString(status,2)}${zigbee.convertToHexString(seconds,2)}}",
    			 "send 0x${device.deviceNetworkId} 1 1", 'delay 100']
                 
	cmds
//  def results = cmds?.collect{ new hubitat.device.HubAction(it,hubitat.device.Protocol.ZIGBEE) };
//	logdebug "sendRawStatus results"+results
//  return results
}

def notifyPanelStatusChanged(status) {
	//TODO: not yet implemented. May not be needed.
}
//------------------------//

def setDisarmed() {
	logdebug ('setDisarm entered')
	setModeHelper("disarmed",0)
	}
def setArmedAway(def delay=0) { setModeHelper("armedAway",delay) }
def setArmedStay(def delay=0) { setModeHelper("armedStay",delay) }
def setArmedNight(def delay=0) { setModeHelper("armedNight",delay) }
//	Hubitat Command set V1.0.1 Feb 21, 2019, 
//on Mar 31, 2019 HE sent disarm 3 times, ignore when mode is correct on device
//on Apr 19, 2019 Not using HSM commands
def disarm(delay=0) 	
	{
	logdebug ('disarm entered')
//	if (device.currentValue('armMode',true) != 'disarmed')
//		setModeHelper("disarmed",0) 
	}
def armAway(def delay=0)
	{
	logdebug ('armAway entered')
//	if (device.currentValue('armMode',true) != 'armedAway')
//		setModeHelper("armedAway",delay) 
	}
def armHome(def delay=0)
	{
	logdebug ('armHome entered')
//	if (device.currentValue('armMode',true) != 'armedStay')
//		setModeHelper("armedStay",delay)
	}
def armNight(def delay=0) 
	{
	logdebug ('armNight entered')
//	if (device.currentValue('armMode',true) != 'armedNight')
//		setModeHelper("armedNight",delay)
	}

def entry(delay=0)
	{
	logdebug "entry entered delay: ${delay}"
//	setEntryDelay(delay)	//disabled until I understand why this is issued when setting away from actiontiles
	}
	
def setEntryDelay(delay) {
//	setModeHelper("entryDelay", delay)
	sendRawStatus(5, delay) // Entry delay beeps
}

def setExitDelay(delay) {
//	setModeHelper("exitDelay", delay)
//	setModeHelper("exitDelay", 0)
	sendRawStatus(10, delay)  // Exit delay
}

private setModeHelper(String armMode, delay) {
	logdebug "In setmodehelper armMode: $armMode delay: $delay"
	sendEvent(name: "armMode", value: armMode)
	if (armMode != 'entryDelay')
		{
		def lastUpdate = formatLocalTime(now())
		sendEvent(name: "lastUpdate", value: lastUpdate, displayed: false)
		}
	sendStatusToDevice(armMode)
}

private setKeypadArmMode(armMode){
	Map mode = [disarmed: '00', armedAway: '03', armedStay: '01', armedNight: '02', entryDelay: '', exitDelay: '']
    if (mode[armMode] != '')
    {
		return ["raw 0x501 {09 01 04 ${mode[armMode]}00}",
				 "send 0x${device.deviceNetworkId} 1 1", 'delay 100']
    }
}

def acknowledgeArmRequest(armMode='0'){
	logtrace "entered acknowledgeArmRequest armMode: ${armMode}"
	List cmds = [
				 "raw 0x501 {09 01 00 0${armMode}}",
				 "send 0x${device.deviceNetworkId} 1 1", "delay 100"
				]
//	def results = cmds?.collect{ new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE) }
//	logtrace "Method: acknowledgeArmRequest(armMode): "+results
//	return results
	cmds

}

def sendInvalidKeycodeResponse(){
	List cmds = [
				 "raw 0x501 {09 01 00 04}",
				 "send 0x${device.deviceNetworkId} 1 1", "delay 100"
				]
				 
	logtrace 'Method: sendInvalidKeycodeResponse(): '+cmds
//	return (collect{ new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE) }) + sendStatusToDevice()
	cmds
	sendStatusToDevice()
}

def beep(def beepLength = settings.beepLength as Integer) {
	if ( beepLength == null )
	{
		beepLength = 1
	}

	def len = zigbee.convertToHexString(beepLength, 2)
//	List cmds = ["raw 0x501 {09 01 04 05${len}}", 'delay 200',
//				 "send 0x${device.deviceNetworkId} 1 1", 'delay 500']
	List cmds = ["raw 0x501 {09 01 04 05${len}}",
				 "send 0x${device.deviceNetworkId} 1 1", 'delay 100']
	cmds
//	return (cmds?.collect{ new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE) }
}

//------Utility methods------//

private String swapEndianHex(String hex) {
	reverseArray(hex.decodeHex()).encodeHex()
}

private byte[] reverseArray(byte[] array) {
	int i = 0;
	int j = array.length - 1;
	byte tmp;
	while (j > i) {
		tmp = array[j];
		array[j] = array[i];
		array[i] = tmp;
		j--;
		i++;
	}
	return array
}
//------------------------//

private testCmd(){
	//logtrace zigbee.parse('catchall: 0104 0501 01 01 0140 00 4F2D 01 00 0000 07 00 ')
	//beep(10)
	//test exit delay
	//logdebug device.zigbeeId
	//testingTesting()
	//discoverCmds()
	//zigbee.configureReporting(1,0x20,0x20,3600,43200,0x01)		//battery reporting
	//["raw 0x0001 {00 00 06 00 2000 20 100E FEFF 01}",
	//"send 0x${device.deviceNetworkId} 1 1"]
	//zigbee.command(0x0003, 0x00, "0500") //Identify: blinks connection light
    
	//logdebug 		//temperature reporting  
    
	return zigbee.readAttribute(0x0020,0x01) + 
		    zigbee.readAttribute(0x0020,0x02) +
		    zigbee.readAttribute(0x0020,0x03)
}

private discoverCmds(){
	List cmds = ["raw 0x0501 {08 01 11 0011}", 'delay 200',
				 "send 0x${device.deviceNetworkId} 1 1", 'delay 500']
	cmds
}

private testingTesting() {
	logdebug "Delay: "+device.currentState("armMode").toString()
	List cmds = ["raw 0x501 {09 01 04 050A}", 'delay 200',
				 "send 0x${device.deviceNetworkId} 1 1", 'delay 500']
	cmds
}

def logdebug(txt)
	{
   	if (logdebugs)
   		log.debug ("${txt}")
    }
def logtrace(txt)
	{
   	if (logtraces)
   		log.trace ("${txt}")
    }


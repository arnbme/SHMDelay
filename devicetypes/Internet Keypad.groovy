/**
 *  Internet Keypad Simulator DTH
 *
 *  Copyright 2018 Arn Burkhoff
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
 *	May 17, 2018	v1.0.0 Created from dummy notification device and Keypad DTH by Mitch Pond, Zack Cornelius
 *
 */
metadata {
	definition (name: "Internet Keypad", namespace: "arnbme", author: "Arn Burkhoff") {
		capability "Notification"

		attribute "armModex", "String"
        attribute "lastUpdate", "String"
		
		command "setDisarmed"
		command "setArmedAway"
		command "setArmedStay"
		command "setArmedNight"
		command "setExitDelay", ['number']
		command "setEntryDelay", ['number']
		command "testCmd"
		command "sendInvalidKeycodeResponse"
		command "acknowledgeArmRequest"
	}


	simulator {
		// TODO: define status and reply messages here
	}

    tiles (scale: 2) 
    	{
        multiAttributeTile(name: "keypad", type:"generic", width:6, height:4, canChangeIcon: true)
        	{
            tileAttribute ("device.armModex", key: "PRIMARY_CONTROL")
            	{            		
                attributeState("disarmed", label:'${currentValue}', icon:"st.Home.home2", backgroundColor:"#44b621")
                attributeState("armedStay", label:'ARMED/STAY', icon:"st.Home.home3", backgroundColor:"#ffa81e")
                attributeState("armedNight", label:'ARMED/NIGHT', icon:"st.Home.home3", backgroundColor:"#ffa81e")
                attributeState("armedAway", label:'ARMED/AWAY', icon:"st.nest.nest-away", backgroundColor:"#d04e00")
            	}
            tileAttribute("device.lastUpdate", key: "SECONDARY_CONTROL") 
            	{
                attributeState("default", label:'Last Used: ${currentValue}')
            	}
        	}
        }	
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"

}

// handle pin commands
def deviceNotification(keycode, armMode) 
	{
	if (keycode=='panic')
		{
		devicePanic ()
		return true;
		}
	def armModex
	log.debug "Executing 'deviceNotification' ${armMode} and pin: ${keycode}"
	sendEvent([name: "codeEntered", value: keycode as String, data: armMode as String, 
				isStateChange: true, displayed: false])
	if (armMode=='0')
		armModex='disarmed'
	else
	if (armMode=='1')
		armModex='armedStay'
	else
	if (armMode=='2')
		armModex='armedNight'
	else
	if (armMode=='3')
		armModex='armedAway'
	else
		armModex='Unknown:'+armMode
		
//	sendEvent([name: "armMode", value: armMode, data: [delay: delay as int], isStateChange: true])
	sendEvent([name: "armModex", value: armModex, displayed: false])
	def lastUpdate = formatLocalTime(now())
	sendEvent(name: "lastUpdate", value: lastUpdate, displayed: false)
	}


// handle Panic request
def devicePanic() 
	{
	log.debug "keypad dth Executing 'devicePanic'"
    sendEvent(name: "contact", value: "open", displayed: true, isStateChange: true)
    runIn(3, "panicContactClose")
	sendEvent([name: "armModex", value: "Panic", displayed: false])
	def lastUpdate = formatLocalTime(now())
	sendEvent(name: "lastUpdate", value: lastUpdate, displayed: false)
	}

def panicContactClose()
	{
	sendEvent(name: "contact", value: "closed", displayed: true, isStateChange: true)
	}

def setDisarmed() {}
def setArmedAway(def delay=0) {}
def setArmedStay(def delay=0) {}
def setArmedNight(def delay=0) {}
def setEntryDelay(delay) {}
def setExitDelay(delay) {}
def testCmd() {}
def sendInvalidKeycodeResponse() {}
def acknowledgeArmRequest(armMode){}

private formatLocalTime(time, format = "EEE, MMM d yyyy @ h:mm:ss a z") {
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


 /*
 *  SHM Delay ModeFix 
 *  Functions: Fix the mode when it is invalid, generally caused when using Dashboard to switch modes
 * 
 *  Copyright 2017 Arn Burkhoff
 * 
 *  Changes to Apache License
 *	4. Redistribution. Add paragraph 4e.
 *	4e. This software is free for Private Use. All derivatives and copies of this software must be free of any charges,
 *	 	and cannot be used for commercial purposes.
 *
 *  Licensed under the Apache License with changes noted above, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Apr 20, 2019 	V0.1.8AH Move sendevent for arm and disarm message here, keypad control in one place
 *	Apr 20, 2019 	V0.1.8AH Move sendevent for exit message here, handle double arming issue with AtomicState
 *	Apr 17, 2019 	V0.1.8H Add code to run system off HSM armStates, caused double armingAway state, double TTS
 *								fixed using atomicState to track hsmState
 *								This module does armstate to mode change.
 *								Note issuing mode change causes a second armingMode, this is a HE BUG
 *	Apr 16, 2019 	V0.1.8H Modified: Add third HSM armed state
 *	Apr 12, 2019 	V0.1.7H Modified: To work in Hubitat
 *				 			need to add mode armedStay he has 4 arm modes
 *
 *	Mar 05, 2019 	V0.1.7  Added: Boolean flag for debug message logging, default false
 *
 *	Jan 06, 2019 	V0.1.6  Added: Support for 3400_G Centralite V3
 *
 * 	Oct 17, 2018	v0.1.5	Allow user to set if entry and exit delays occur for a state/mode combination
 *								
 * 	Apr 24, 2018	v0.1.4	For Xfinity and Centralite model 3400 keypad on armed (Home) modes 
 *								add device icon button to light Stay (Entry Delay) or Night (Instant Intrusion)
 *								
 * 	Mar 11, 2018    v0.1.3  add logging to notifications when mode is changed. 
 *								App issued changes are not showing in PhoneApp notifications
 *								Assumed system would log this but it does not
 * 	Sep 23, 2017    v0.1.2  Ignore alarm changes caused by True Entry Delay in SHM Delay Child
 * 	Sep 05, 2017    v0.1.1  minor code change to allow this module to run stand alone
 * 	Sep 02, 2017    v0.1.0  add code to fix bad alarmstate set by unmodified Keypad module
 * 	Sep 02, 2017    v0.1.0  Repackage logic that was in parent into this module for better reliability
 *					and control
 * 	Aug 26/27, 2017 v0.0.0  Create 
 *
 */

definition(
    name: "SHM Delay ModeFix",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "(${version()}) Fix the ST Mode and or Alarm State when using ST Dashboard to change AlarmState or Mode",
    category: "My Apps",
	parent: "arnbme:SHM Delay",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    singleInstance: true)

preferences {
	page(name: "pageOne", nextPage: "pageOneVerify")
	page(name: "pageOneVerify")
	page(name: "pageTwo")
	page(name: "aboutPage", nextPage: "pageOne")
}

def version()
	{
	return "0.1.8H";
	}

def pageOne(error_msg)
	{
	dynamicPage(name: "pageOne", title: "For each alarm state, set valid modes and default modes.", install: false, uninstall: true)
		{
		section
			{
			if (error_msg instanceof String )
				{
				paragraph error_msg
				}
			else
				paragraph "Caution! Wrong settings may create havoc. If you don't fully understand Alarm States and Modes, read the Introduction and use the defaults!"
			href(name: "href",
			title: "Introduction",
			required: false,
			page: "aboutPage")
			}
		section ("Debugging messages")
			{
			input "logDebugs", "bool", required: false, defaultValue:false,
				title: "Log debugging messages? Normally off/false"
			}
		section ("Alarm State: Disarmed / Off")
			{
			input "offModes", "mode", required: true, multiple: true, defaultValue: "Home",
				title: "Valid Modes for: Disarmed"
			input "offDefault", "mode", required: true, defaultValue: "Home",
				title: "Default Mode for: Disarmed"
			}	
		section ("Alarm State: Armed (Away)")
			{
			if (away_error_data instanceof String )
				{
				paragraph away_error_data
				}
			input "awayModes", "mode", required: true, multiple: true, defaultValue: "Away", submitOnChange: true,
				title: "Valid modes for: Armed Away"
			input "awayDefault", "mode", required: true, defaultValue: "Away",
				title: "Default Mode for Armed Away"
/*			awayModes.each
				{
				input "awayExit${it.value}", "bool", required: true, defaultValue: true,
					title: "Create Exit Delay for Armed (Away) ${it.value} mode"
				input "awayEntry${it.value}", "bool", required: true, defaultValue: true,
					title: "Create Entry Delay for Armed (Away) ${it.value} mode"
				}	
*/			}	
		section ("Alarm State: Armed (Night)")
			{
			input "nightModes", "mode", required: true, multiple: true, defaultValue: "Night", submitOnChange: true,
				title: "Valid Modes for Armed Night"
			input "nightDefault", "mode", required: true, defaultValue: "Night",
				title: "Default Mode for Armed Night"
/*			nightModes.each
				{
				input "nightExit${it.value}", "bool", required: true, defaultValue: false,
					title: "Create Exit Delay for Armed (Night) ${it.value} mode"
				input "nightEntry${it.value}", "bool", required: true, defaultValue: true,
						title: "Create Entry Delay for Armed (Night) ${it.value} mode"
				}	
*/			}	
		section ("Alarm State: Armed (Home) aka Stay")
			{
			input "stayModes", "mode", required: true, multiple: true, defaultValue: "Stay", submitOnChange: true,
				title: "Valid Modes for Armed Home"
			input "stayDefault", "mode", required: true, defaultValue: "Stay",
				title: "Default Mode for Armed Home"
/*			stayModes.each
				{
				input "stayExit${it.value}", "bool", required: true, defaultValue: false,
					title: "Create Exit Delay for Armed (Home) ${it.value} mode"
				input "stayEntry${it.value}", "bool", required: true, defaultValue: true,
					title: "Create Entry Delay for Armed (Home) ${it.value} mode"
				}	
*/			}	
		section
			{
			paragraph "SHM Delay Modefix ${version()}"
			}

		}	
	}	

def pageOneVerify() 				//edit page One
	{

//	Verify disarm/off data
	def off_error="Disarmed / Off Default Mode not defined in Valid Modes"
	def children = offModes
	children.each
		{ child ->
		if (offDefault == child)
			{
			off_error=null
			}
		}
	
//	Verify Away data
	def away_error="Armed (Away) Default Mode not defined in Valid Modes"
	children = awayModes
	children.each
		{ child ->
		if (awayDefault == child)
			{
			away_error=null
			}
		}

//	Verify Stay data
	def stay_error="Armed (Home) Default Mode not defined in Valid Modes"
	children = stayModes
	children.each
		{ child ->
		if (stayDefault == child)
			{
			stay_error=null
			}
		}

	if (off_error == null && away_error == null && stay_error == null)
		{
		pageTwo()
		}
	else	
		{
		def error_msg=""
		def newline=""
		if (off_error>"")
			{
			error_msg=off_error
			newline="\n"
			}
		if (away_error >"")
			{
			error_msg+=newline + away_error
			newline="\n"
			}	
		if (stay_error >"")
			{
			error_msg+=newline + stay_error
			newline="\n"
			}
		pageOne(error_msg)
		}
	}

def pageTwo()
	{
	dynamicPage(name: "pageTwo", title: "Mode settings verified, press 'Done/Save' to install, press '<' to change, ", install: true, uninstall: true)
		{
/*		section
			{
			href(name: "href",
			title: "Introduction",
			required: false,
			page: "aboutPage")
			}
*/		section ("Alarm State: Disarmed / Off")
			{
			input "offModes", "mode", required: true, multiple: true, defaultValue: "Home",
				title: "Valid Modes for: Disarmed"
			input "offDefault", "mode", required: true, defaultValue: "Home",
				title: "Default Mode for: Disarmed"
			}	
		section ("Alarm State: Armed (Away)")
			{
			input "awayModes", "mode", required: true, multiple: true, defaultValue: "Away", submitOnChange: true,
				title: "Valid modes for: Armed Away"
			input "awayDefault", "mode", required: true, defaultValue: "Away",
				title: "Default Mode: Armed Away"
/*			awayModes.each
				{
				input "awayExit${it.value}", "bool", required: true, defaultValue: true,
					title: "Create Exit Delay for Armed (Away) ${it.value} mode"
				input "awayEntry${it.value}", "bool", required: true, defaultValue: true,
					title: "Create Entry Delay for Armed (Away) ${it.value} mode"
				}	
*/			}	
		section ("Alarm State: Armed (Night)")
			{
			input "nightModes", "mode", required: true, multiple: true, defaultValue: "Night",  submitOnChange: true,
				title: "Valid Modes for Armed Night"
			input "nightDefault", "mode", required: true, defaultValue: "Night",
				title: "Default Mode for Armed Night"
/*			stayModes.each
				{
				input "nightExit${it.value}", "bool", required: true, defaultValue: false,
					title: "Create Exit Delay for Armed (Night) ${it.value} mode"
				input "nightEntry${it.value}", "bool", required: true, defaultValue: false,
					title: "Create Entry Delay for Armed (Night) ${it.value} mode"
				}
*/			}	
		section ("Alarm State: Armed (Home) aka Stay")
			{
			input "stayModes", "mode", required: true, multiple: true, defaultValue: "Stay",  submitOnChange: true,
				title: "Valid Modes for Armed Home"
			input "stayDefault", "mode", required: true, defaultValue: "Stay",
				title: "Default Mode for Armed Home"
/*			stayModes.each
				{
				input "stayExit${it.value}", "bool", required: true, defaultValue: false,
					title: "Create Exit Delay for Armed (Home) ${it.value} mode"
				input "stayEntry${it.value}", "bool", required: true, defaultValue: false,
					title: "Create Entry Delay for Armed (Home) ${it.value} mode"
				}	
				
*/			}	

		section
			{
			paragraph "SHM Delay Modefix ${version()}"
			}
		}
	}	

	
def aboutPage()
	{
	dynamicPage(name: "aboutPage", title: "Introduction")
		{
		section 
			{
			paragraph "Have you ever wondered why Mode restriced Rule Machine rules sometimes fail to execute, or execute when they should not?\n\n"+
			"Perhaps you conflated HSM AlarmState and Mode, however they are separate and independent settings, "+
			"and when Alarm State is changed---surprise, Mode does not change!\n\n" +
			"This app changes the Mode when the HSM Alarm State changes. It also triggers most of the app's TTS messaging.\n\n"+
			"HSM changes the Alarm State when the Mode changes"
			}
		}
	}



def installed() {
    log.info "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.info "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() 
	{
	subscribe(location, 'hsmStatus', alarmStatusHandler)
	}


def alarmStatusHandler(evt)
	{
	def theAlarm = evt.value as String				//curent alarm state
	def lastAlarm = atomicState?.hsmstate
	atomicState.hsmstate=theAlarm
	def theMode = location.currentMode as String	//warning without string parameter it wont match
	logdebug "ModeFix alarmStatusHandler entered, HSM state: ${theAlarm}, lastAlarm: ${lastAlarm} Mode: ${theMode} "
//	logdebug "ModeFix alarmStatusHandler entered, HSM state: ${theAlarm}, Mode: ${theMode}"
//	Fix the mode to match the Alarm State. 
	if (theAlarm=="disarmed" || theAlarm=="allDisarmed")
		{
		parent.globalKeypadDevices.setDisarmed()
		ttsDisarmed()
		if (!offModes.contains(theMode))
			{
			setLocationMode(offDefault)
			}
		}
	else
	if (theAlarm=="armedAway" || theAlarm=="armingAway")
		{
		if (theAlarm=="armedAway")
			{
			parent.globalKeypadDevices.setArmedAway()
			ttsArmed(theAlarm)
			}
		else 
		if (theAlarm != lastAlarm)
			{
			parent.globalKeypadDevices.setExitDelay(evt.jsonData.seconds)
			ttsExit(evt.jsonData.seconds)
			}
		if (!awayModes.contains(theMode))
			{
			setLocationMode(awayDefault)
			}
		}
	else
	if (theAlarm=="armedNight" || theAlarm=="armingNight")
		{
		if (theAlarm=="armedNight")
			{
			parent.globalKeypadDevices.each
				{
				if (['3400','3400-G'].contains(it.data.model))
					it.setArmedNight()
				else	
					it.setArmedStay()			//non Centralite keypads have 3 mode lights, light partial
				}
			ttsArmed(theAlarm)
			}
		else
		if (theAlarm != lastAlarm)
			{
			parent.globalKeypadDevices.setExitDelay(evt.jsonData.seconds)
			ttsExit(evt.jsonData.seconds)
			}
		if (!nightModes.contains(theMode))
			{
			setLocationMode(nightDefault)
			}
		}
	else
//	This is equivalent to ST Stay mode		
	if (theAlarm=="armedHome" || theAlarm == "armingHome")
		{
		if (theAlarm=="armedHome")
			{
			parent.globalKeypadDevices.setArmedStay()
			ttsArmed(theAlarm)
			}
		else
		if (theAlarm != lastAlarm)
			{
			parent.globalKeypadDevices.setExitDelay(evt.jsonData.seconds)
			ttsExit(evt.jsonData.seconds)
			}
		if (!stayModes.contains(theMode))
			{
			setLocationMode(stayDefault)
			}
		}
	else
		{
		log.error "ModeFix alarmStatusHandler Unknown alarm mode: ${theAlarm}"
		return false
		}
	}
	
def ttsExit(delay)
	{
	logdebug "ttsExit delay: $delay"
	def locevent = [name:"shmdelaytalk", value: "exitDelay", isStateChange: true,
		displayed: true, descriptionText: "Issue exit delay talk event", linkText: "Issue exit delay talk event",
		data: delay]	
	sendLocationEvent(locevent)
	}

def ttsDisarmed()
	{
	logdebug "ttsDisarmed"
	def locevent = [name:"shmdelaytalk", value: "disarm", isStateChange: true,
		displayed: true, descriptionText: "Issue disarm talk event", linkText: "Issue disarm delay talk event",
		data: 'none']	
	sendLocationEvent(locevent)
	}

def ttsArmed(theAlarm)
	{
	logdebug "ttsArmed"
	def locevent = [name:"shmdelaytalk", value: "armed", isStateChange: true,
		displayed: true, descriptionText: "Issue armed talk event", linkText: "Issue armed delay talk event",
		data: theAlarm]	
	sendLocationEvent(locevent)
	}
	
def logdebug(txt)
	{
   	if (logDebugs)
   		log.debug ("${txt}")
    }	

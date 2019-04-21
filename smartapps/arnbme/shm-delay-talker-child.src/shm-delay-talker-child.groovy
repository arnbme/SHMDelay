/**
 *  SHM Delay Talker Child
 *  Supplements Big Talker adding speech when SHMDelay enters the Exit or Entry delay time period
 *		For LanNouncer Device: Chime, TTS text, Chime
 *		For speakers (such as Sonos)  TTS text
 *	Supports TTS devices and speakers
 *	When devices use differant messages, install multiple copies of this code
 *	When speakers need different volumes, install multiple copies of this code
 *
 *
 *  Copyright 2017 Arn Burkhoff
 *
 * 	Changes to Apache License
 *	4. Redistribution. Add paragraph 4e.
 *	4e. This software is free for Private Use. All derivatives and copies of this software must be free of any charges,
 *	 	and cannot be used for commercial purposes.
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
 *	Apr 21, 2019 v2.1.4AH Fix missing NEXT Button on pageone, add nextPage: to dynamic page 1
 * 	Apr 21, 2019 v1.0.4AH set as single instance remove label
 * 	Apr 21, 2019 v1.0.4AH All keypad messages in one place for easy setup. Produce arm and disarm messages
 * 	Apr 20, 2019 v1.0.4AH subscribe to hsmAlert events for pin entry messages
 * 	Apr 20, 2019 v1.0.4H Modify for hubitat and fully tts
 * 	Dec 17, 2018 v1.0.4 Change speaker capability audioNotification to musicPlayer. Did not select Sonos speakers
 * 	Nov 04, 2018 v1.0.3 Add support for generic quiet time per user request on messages
 *						Delayed messages are super delayed by unknown cloud processing error, allow for no chime and instant speak
 * 	Oct 21, 2018 v1.0.2	Support Arming Canceled messages from SHM Delay 
 * 	Jul 05, 2018 v1.0.1	correct non standard icon 
 * 	Jul 04, 2018 v1.0.1	Check for non Lannouner TTS devices and when true eliminate chime command 
 *	Jun 26, 2018 V1.0.0 Create from standalone module Keypad ExitDelay Talker
 */
definition(
    name: "SHM Delay Talker Child",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "(${version()}) Speak during SHM Delay Exit and Entry Delay",
    category: "My Apps",
    parent: "arnbme:SHM Delay",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    singleInstance: true)

def version()
	{
	return "1.0.4AH";
	}

preferences {
	page(name: "pageZeroVerify")
	page(name: "pageZero", nextPage: "pageZeroVerify")
	page(name: "pageOne", nextPage: "pageOneVerify")
	page(name: "pageOneVerify")
	page(name: "pageTwo")		//recap page when everything is valid. No changes allowed.
	}

def pageZeroVerify()
//	Verify this is installed as a child
	{
	if (parent && parent.getInstallationState()=='COMPLETE')
		{
		pageOne()
		}
	else
		{
		pageZero()
		}
	}	

def pageZero()
	{
	dynamicPage(name: "pageZero", title: "This App cannot be installed", uninstall: true, install:false)
		{
		section
			{
			paragraph "This SmartApp, SHM Delay Talker, must be used as a child app of SHM Delay."
			}
		}
	}	

def pageOne()
	{
	dynamicPage(name: "pageOne", title: "Talker Messages and Devices", install: false, uninstall: true, nextPage: "pageOneVerify")
		{
		section("The SHM Delay Message Settings")
			{
			if (state.error_data)
				{
				paragraph "${state.error_data}"
				state.remove("error_data")
				}
			input "logDebugs", "bool", required:true, defaultValue:false,
				title: "Log debugging messages? Normally off/false"
			input "theExitMsgKypd", "string", required: false, title: "Exit message: %nn replaced with delay seconds", 
				defaultValue: "Alarm system is arming in %nn seconds. Please exit the facility"
			input "theEntryMsg", "string", required: false, title: "Entry message: %nn replaced with delay seconds", 
				defaultValue: "Please enter your pin on the keypad"
			input "theArmMsg", "string", required: false, title: "Armed message: %hsmStatus replaced with HSM Arm State", 
					defaultValue: "Alarm System is now armed in %hsmStatus Mode"
			input "theDisarmMsg", "string", required: false, title: "Disarm message", 
					defaultValue: "System Disarmed"
			input(name: 'theStartTime', type: 'time', title: 'Do not talk: Start Time', required: false)
			input(name: 'theEndTime', type: 'time', title: 'Do not talk: End Time', required: false)
			input "theSoundChimes", "bool", defaultValue: true, required: false,
				title: "Sound TTS Chimes with messages Default: On/True"
			input "theTTS", "capability.speechSynthesis", required: false, multiple: true, submitOnChange: true,
				title: "LanNouncer/DLNA TTS Devices"
			input "theSpeakers", "capability.musicPlayer", required: false, multiple: true, submitOnChange: true,
				title: "Speaker Devices?"
			input "theVolume", "number", required: true, range: "1..100", defaultValue: 40,
				title: "Speaker Volume Level from 1 to 100"
			}
		}
	}	

def pageOneVerify() 				//edit page one info, go to pageTwo when valid
	{
	def error_data = ""
	if (theStartTime>"" && theEndTime>"")
		{}
	else
	if (theStartTime>"")
		error_data="Please set do not talk end time or clear do not talk start time"
	else	
	if (theEndTime>"")
		error_data="Please set do not talk start time or clear do not talk end time"

	if (error_data!="")
		{
		state.error_data=error_data.trim()
		pageOne()
		}
	else
		{
		pageTwo()
		}
	}	

//	This page summarizes the data prior to save	
def pageTwo(error_data)
	{
	dynamicPage(name: "pageTwo", title: "Verify settings then tap Done, or go back (tap <) to change settings", install: true, uninstall: true)
		{
		def chimes=false
		def chimetxt='(Chime) '
		try 
			{chimes=theSoundChimes}
		catch(Exception e)
			{}
		if (!chimes)
			chimetxt=''
		section
			{
			if (theExitMsgKypd)
				paragraph "The Exit Delay Message:\n${chimetxt}${theExitMsgKypd}"
			else	
				paragraph "The Exit Delay Message is not defined"
			if (theEntryMsg)
				paragraph "The Entry Delay Message:\n${chimetxt}${theEntryMsg}"
			else	
				paragraph "The Entry Delay Message is not defined"
			if (theArmMsg)
				paragraph "The Armed Message:\n${theArmMsg}"
			else	
				paragraph "The Armed Message is not defined"
			if (theDisarmMsg)
				paragraph "The Disarm Message:\n${theDisarmMsg}"
			else	
				paragraph "The Disarm Message is not defined"
			if (theStartTime>"" && theEndTime>"")
				paragraph "Quiet time active from ${theStartTime.substring(11,16)} to ${theEndTime.substring(11,16)}"	
			else
				paragraph "Quiet time is inactive"

			if (!chimes)
				paragraph "Chimes do not sound with messages"	
			if (theTTS)
				paragraph "The Text To Speech Devices are ${theTTS}"
			else	
				paragraph "No Text To Speech Devices are defined"
			if (theSpeakers)
				{
				paragraph "The Text To Speech Devices are ${theSpeakers}"
				paragraph "The Speaker Volume Level is ${theVolume}"
				}
			else	
				paragraph "No Speaker Devices are defined"
			paragraph "Module SHM Delay Talker Child ${version()}"
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

def initialize() {
	subscribe(location, "hsmAlert", alertHandler)
	subscribe(location, "shmdelaytalk", TalkerHandler)
	}

def alertHandler(evt)
	{
	logdebug("alertHandler entered, event: ${evt.value}")
	if (['intrusion-delay','intrusion-home-delay','intrusion-night-delay'].contains(evt.value))
		{
		parent.globalKeypadDevices.setEntryDelay(evt.jsonData.seconds)
		TalkerHandler([value: 'entryDelay', data: evt.jsonData.seconds])
		}
	}
	
def TalkerHandler(evt)
	{
	logdebug("TalkerHandler entered, event: ${evt.value} ${evt?.data}")
	def delaydata=evt?.data			//get the delay time 
	def msgout

//	1.0.3 Nov 4, 2018 check time values for quiet
	if (theStartTime>"" && theEndTime>"")
		{
		def between = timeOfDayIsBetween(theStartTime.substring(11,16), theEndTime.substring(11,16), new Date(), location.timeZone)
		if (between)
			{
//			logdebug ("it is quiet time")	
			return false
			}
		}

	if (evt.value=="entryDelay" && theEntryMsg>"")
		{
		def delaydatax=delaydata as String		//throws casting error if not done
		if (delaydatax>"")
			msgout=theEntryMsg.replaceAll("%nn",delaydatax)
		else
			msgout=theEntryMsg
		log.debug msgout	
		if (theTTS)
			{
			if (theSoundChimes)
				theTTS.chime()
			runInMillis(1800, ttsDelay, [data: [tts: msgout]])
			}	
		if (theSpeakers)
			{
			theSpeakers.playTextAndResume(msgout,theVolume)
			}
		}
	else
	if (evt.value=="exitDelay" && theExitMsgKypd>"")
		{
		if (delaydata>"")
			msgout=theExitMsgKypd.replaceAll("%nn",delaydata)
		else
			msgout=theExitMsgKypd
		if (theTTS)
			{
			if (theSoundChimes)
				{
				theTTS.chime()
				runInMillis(1800, ttsDelay, [data: [tts: msgout]])
				}
			else		
				{theTTS.speak(msgout)}
			}
		if (theSpeakers)
			{
			theSpeakers.playTextAndResume(msgout,theVolume)
			}
		}
	else
	if (evt.value=="disarm" && theDisarmMsg>"")
		{
		if (theTTS)
			{theTTS.speak(theDisarmMsg)}					
		if (theSpeakers)
			{theSpeakers.playTextAndResume(theDisarmMsg,theVolume)}
		}
	else
	if (evt.value=="armed" && theArmMsg>"")
		{
		def hsmState = [armedAway: "Away", armedHome: "Home", armedNight: "Night"][delaydata] ?: delaydata
		msgout=theArmMsg.replaceAll("%hsmStatus",hsmState)
		if (theTTS)
			{theTTS.speak(msgout)}					
		if (theSpeakers)
			{theSpeakers.playTextAndResume(msgout,theVolume)}
		}
	}

def ttsDelay(map)
	{
	log.debug "ttsDelay entered: ${map.tts}"
	theTTS.speak(map.tts)
	}

def logdebug(txt)
	{
   	if (logDebugs)
   		log.debug ("${txt}")
    }		

/**
 *  SHMDelay Buzzer
 *  Created for Konnected Users to sound their Buzzer for entry and exit delays
 *  but supports using anything with an alarm capability
 *
 *  Copyright 2019 Arn Burkhoff
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
 * 	Nov 12, 2018 v1.0.0 Create from SHM Delay Talker
 */
definition(
    name: "SHM Delay Buzzer",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "(${version()}) Use Konnected Buzzer for exit and entry delay sounds",
    category: "My Apps",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png")

def version()
	{
	return "1.0.1";
	}

preferences {
	page(name: "pageOne")
	}


def pageOne()
	{
	dynamicPage(name: "pageOne", title: "Select Devices To Set On", install: true, uninstall: true)
		{
		section("The Devices")
			{
			input "theBuzzers", "capability.switch", required: false, multiple: true
				title: "Alarm/Buzzer Devices?"
			}
		}
	}	


def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(location, "shmdelaytalk", BuzzerHandler)
	subscribe(location, "alarmSystemStatus", AlarmStatusHandler)
	}

def BuzzerHandler(evt)
	{
	def alarm = location.currentState("alarmSystemStatus")
	def alarmstatus = alarm?.value
	def delayMilli=0
	log.debug("BuzzerHandler entered, event: ${evt.value} ${evt?.data} ${alarmstatus}")
	
	if (evt.value=="exitDelayNkypd")
		{
		if (evt?.data)
			{
			delayMilli= evt.data as Integer
			delayMilli= delayMilli * 1000
			}
		else
			return false
		log.debug "delayMilli ${delayMilli}"		//testing code
//		delayMilli=3000
//		log.debug "delayMilli ${delayMilli}"
		if (alarmstatus=='away')
//			system already in away mode shut device in delay seconds
			{
           	log.debug("BuzzerHandler non keypad exit delay requested, system in away mode")
			theBuzzers.on()
			theBuzzers.off([delay: delayMilli])
			}
		else
//			it is likely ST is running slow and is not in away mode, this really should not occur
//			so issue a 2 second delayed on
//			followed by a delay + 4 seconds to shut it
			{
           	log.debug("BuzzerHandler non keypad exit delay requested, but system not in away mode")
			theBuzzers.on([delay: 2000])
            delayMilli=delayMilli+4000
			theBuzzers.off([delay: delayMilli])  
			}
		}
	else
	if (evt.value=="entryDelay" || evt.value=="exitDelay")
		theBuzzers.on()
	else
	if (evt.value=="ArmCancel")
		theBuzzers.off()
	}

def AlarmStatusHandler(evt)
	{
	log.debug("SHM changed to ${evt.value}, Device off")
	theBuzzers.off()
	}	
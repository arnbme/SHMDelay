/**
 *  originally created for Konnected Users to sound the Buzzer for entry and exit delays
 *  but supports using anything with a switch/On/Off capability
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
 * 	Jan 01, 2019 v1.1.0 optionally issue both command if available instead of on command, gives long duration sound
 * 	Nov 17, 2018 v1.0.2 rename and clean up for production
 * 	Nov 14, 2018 v1.0.2 add logic for: nonkeypad exit set and system not in away mode
 * 	Nov 12, 2018 v1.0.1 fix non keypad on / off and attempt to compensate slow ST cloud
 * 	Nov 12, 2018 v1.0.0 Create from SHM Delay Talker
 */
definition(
    name: "SHM Delay BuzzerSwitch",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "(${version()}) Set Konnected Buzzer, Switch or Bulb ON during SHm Delay exit and entry delay",
    category: "My Apps",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png")

def version()
	{
	return "1.1.0";
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
				title: "Devices with Switch/On/Off capbability"
			input "useBoth", "bool", title: "My Konnected buzzer issues a short beep, I want a long beep",
					defaultValue: false, required: false
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
	log.debug "BuzzerHandler entered"
	def alarm = location.currentState("alarmSystemStatus")
	def alarmstatus = alarm?.value
	def delayMilli=0
	if (evt?.data)
		{
		delayMilli= evt.data as Integer
		delayMilli= delayMilli * 1000
		}
	else
		return false
	
	if (evt.value=="exitDelayNkypd")
		{
		log.debug "BuzzerHandler entered, event: ${evt.value} ${evt?.data} ${alarmstatus}"
//		log.debug "delayMilli ${delayMilli}"		//testing code
		if (alarmstatus=='away')
//			system in away mode, normal processing
			{
           	log.debug "BuzzerHandler non keypad exit delay requested, system in away mode" 
			OnOrBoth()
			theBuzzers.off([delay: delayMilli])
			}
		else
//			it is likely ST is running slow and is not in away mode, this really should not occur
//			but this is ST so it likely will happen. Deal with it
//			so cycle up to 5 times or 9 seconds, when away set or end of cycle issue commands.
			{
			log.debug "issue initial cycle"
			unschedule(issueDelayedOn)		//just incase a delayed on is pending
			def switch_map=[data:[cycles:5, delayMilli: delayMilli]]
			runIn(1, issueDelayedOn, switch_map)
			}
		}
	else
	if (evt.value=="exitDelay")
		{
		OnOrBoth()
		theBuzzers.off([delay: delayMilli])
		}
	else
	if (evt.value=="entryDelay")
		OnOrBoth()
	else
	if (evt.value=="ArmCancel")
		theBuzzers.off()
	}

def AlarmStatusHandler(evt)
	{
	log.debug("SHM changed to ${evt.value}, from ${evt.source} ${evt.device}, set devices off")
	if (evt.value != 'away')
		{
		theBuzzers.off()
		unschedule(issueDelayedOn)	//Attempt to handle any hanging delayed requests 
		}
	}
	
	
def issueDelayedOn(switch_map)
/*	When system is not armed: Wait for it to arm, then issue commands
**	Limit time to 5 cycles around 9 seconds of waiting maximum
*/		
	{
	def alarm = location.currentState("alarmSystemStatus")	//get ST alarm status
	def alarmstatus = alarm.value
	if (alarmstatus != "away")
		{
		log.debug "issueDelayedOn entered $switch_map"
		if (switch_map.cycles > 1)
			{
			def cycles=switch_map.cycles-1
			def delayMilli=switch_map.delayMilli
			def newswitch_map=[data:[cycles: cycles, delayMilli: delayMilli]]
			runIn(2, issueDelayedOn, newswitch_map)
			return false
			}
		}
	
	OnOrBoth()
	theBuzzers.off([delay: delayMilli])
	}

def OnOrBoth()
	{
	if (useBoth)
		{
		theBuzzers.each()
			{
			if (it.hasCommand("both"))
				it.both()
			else	
				it.on()
			}	
		}
	else
		theBuzzers.on()
	}
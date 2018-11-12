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
	return "1.0.0";
	}

preferences {
	page(name: "pageOne")
	}


def pageOne()
	{
	dynamicPage(name: "pageOne", title: "Select Alarm Devices/Buzzers", install: true, uninstall: true)
		{
		section("The Alarm Devices / Buzzers")
			{
			input "theBuzzers", "capability.alarm", required: false, multiple: true
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
	log.debug("BuzzerHandler entered, event: ${evt.value} ${evt?.data}")
	if (evt.value=="entryDelay" || evt.value=="exitDelay" || evt.value=="exitDelayNkypd")
		theBuzzers.alarm()
	else
	if (evt.value=="ArmCancel")
		theBuzzers.off()
	}

def AlarmStatusHandler(evt)
	{
	log.debug("SHM changed to ${evt.value}, Buzzer off")
	theBuzzers.off()
	}	
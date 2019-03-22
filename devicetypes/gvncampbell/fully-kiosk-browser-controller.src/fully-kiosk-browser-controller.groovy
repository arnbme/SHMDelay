metadata {
    definition (name: "Fully Kiosk Browser Controller", namespace: "GvnCampbell", author: "Gavin Campbell") {
		capability "Tone"
		capability "Speech Synthesis"
		capability "AudioVolume"
        capability "Refresh"
        command "chime"
        command "doorbell"
		command "launchAppPackage"
		command "bringFullyToFront"
    }

	preferences {
		input(name:"serverIP",type:"string",title:"Server IP Address",required:true,displayDuringSetup: true)
		input(name:"serverPort",type:"string",title:"Server Port",defaultValue:2323,required:true,displayDuringSetup: true)
		input(name:"serverPassword",type:"string",title:"Server Password",required:true,displayDuringSetup: true)
		input(name:"toneFile",type:"string",title:"Tone Audio File URL",required:false,displayDuringSetup: true)
		input(name:"appPackage",type:"string",title:"Application to Launch",required:false,displayDuringSetup: true)
		input(name:"loggingLevel",type:"enum",title:"Logging Level",description:"Set the level of logging.",options:["none","debug","trace","info","warn","error"],defaultValue:"debug",required:true,displayDuringSetup: true)
    }
    tiles
    	{
        standardTile("speak", "device.speech", inactiveLabel: false, decoration: "flat") 
        	{
            state "default", label:'Speak', action:"Speech Synthesis.speak", icon:"st.Electronics.electronics13"
        	}
        standardTile("beep", "device.tone", inactiveLabel: false, decoration: "flat")
        	{
            state "default", label:'Tone', action:"tone.beep", icon:"st.Entertainment.entertainment2"
        	}
    	}

	}

// *** [ Initialization Methods ] *********************************************
def installed() {
	def logprefix = "[installed] "
    logger logprefix
    initialize()
}
def updated() {
	def logprefix = "[updated] "
    logger logprefix
	initialize()
}
def initialize() {
	def logprefix = "[initialize] "
    logger logprefix
}

// *** [ Device Methods ] *****************************************************
def doorbell() {}
def beep(text="Fully Kiosk Device Handler") {
	def logprefix = "[beep] "
    logger(logprefix,"trace")
	sendCommandPost("cmd=playSound&url=${toneFile}")
}
def chime() {beep()}
def launchAppPackage() {
	def logprefix = "[launchAppPackage] "
    logger(logprefix,"trace")
	sendCommandPost("cmd=startApplication&package=${appPackage}")
}
def bringFullyToFront() {
	def logprefix = "[bringFullyToFront] "
    logger(logprefix,"trace")
	sendCommandPost("cmd=toForeground")
}
def speak(text="Fully Kiosk TTS Device Handler") {
	def logprefix = "[speak] "
	logger(logprefix+"text:${text}","trace")
	sendCommandPost("cmd=textToSpeech&text=${java.net.URLEncoder.encode(text, "UTF-8")}")
}
def setVolume(volumeLevel) {
	def logprefix = "[setVolume] "
	logger(logprefix+"volumeLevel:${volumeLevel}")
	for (i=1;i<=10;i++) {
		sendCommandPost("cmd=setAudioVolume&level=${volumeLevel}&stream=${i}")
	}
	sendEvent([name:"volume",value:volumeLevel])
}
def volumeUp() {
	def logprefix = "[volumeUp] "
	logger(logprefix)
	def newVolume = device.currentValue("volume")
	if (newVolume) {
		newVolume = newVolume.toInteger() + 10
		newVolume = Math.min(newVolume,100)
		setVolume(newVolume)
	}
}
def volumeDown() {
	def logprefix = "[volumeDown] "
	logger(logprefix)
	def newVolume = device.currentValue("volume")
	if (newVolume) {
		newVolume = newVolume.toInteger() - 10
		newVolume = Math.max(newVolume,0)
		setVolume(newVolume)
	}
}
def mute() {
	def logprefix = "[mute] "
	logger(logprefix)
}
def unmute() {
	def logprefix = "[unmute] "
	logger(logprefix)
}
def refresh() {
  	def logprefix = "[refresh] "
  	logger logprefix
	sendCommandPost("cmd=deviceInfo")
}

// *** [ Communication Methods ] **********************************************
/*	HE Methods
def sendCommandPost(cmdDetails="") {
	def logprefix = "[sendCommandPost] "
	logger(logprefix+"cmdDetails:${cmdDetails}","trace")
	def postParams = [
		uri: "http://${serverIP}:${serverPort}/?type=json&password=${serverPassword}&${cmdDetails}",
		requestContentType: 'application/json',
		contentType: 'application/json'
	]
	logger(logprefix+postParams)
	asynchttpPost("sendCommandCallback", postParams, null)
}
def sendCommandCallback(response, data) {
	def logprefix = "[sendCommandCallback] "
    logger(logprefix+"response.status: ${response.status}","trace")
	if (response?.status == 200) {
		logger(logprefix+"response.data: ${response.data}","trace")
		def jsonData = parseJson(response.data)
		if (jsonData?.ip4 || jsonData?.status == "OK") {
			logger(logprefix+"Updating last activity.","trace")
			sendEvent([name:"refresh"])
		}
	}
}
*/
//	[SmartThing Communications] *********************************************** 
def sendCommandPost(cmdDetails="") 
	{
	def logprefix = "[sendCommandPost] "
	logger(logprefix+"cmdDetails:${cmdDetails} to ${serverIP}:${serverPort}","trace")
    if (serverIP?.trim()) 
    	{
        def hosthex = convertIPtoHex(serverIP)
        def porthex = convertPortToHex(serverPort)
        device.deviceNetworkId = "$hosthex:$porthex"
        def headers = [:] 
        headers.put("HOST", "$serverIP:$serverPort")
        def method = "POST"
        def hubAction = new physicalgraph.device.HubAction(
            method: method,
            path: "/?type=json&password=${serverPassword}&${cmdDetails}",
            headers: headers
            );
		logger(logprefix+"hubAction: ${hubAction}","trace")
        return hubAction
		}
	}
private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
//    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04X', port.toInteger() )
//    log.debug hexport
    return hexport
}	

// *** [ Logger ] *************************************************************
private logger(loggingText,loggingType="debug") {
	def internalLogging = false
	def internalLoggingSize = 500
	if (internalLogging) { if (!state.logger) {	state.logger = [] }	} else { state.logger = [] }

	loggingType = loggingType.toLowerCase()
	def forceLog = false
	if (loggingType.endsWith("!")) {
		forceLog = true
		loggingType = loggingType.substring(0, loggingType.length() - 1)
	}
	def loggingTypeList = ["trace","debug","warn","info","error"]
	if (!loggingTypeList.contains(loggingType)) { loggingType="debug" }
	if ((!loggingLevel||loggingLevel=="none") && loggingType == "error") {
	} else if (forceLog) {
	} else if (loggingLevel == "debug" || (loggingType == "error")) {
	} else if (loggingLevel == "trace" && (loggingType == "trace" || loggingType == "info")) {
	} else if (loggingLevel == "info"  && (loggingType == "info")) {
	} else if (loggingLevel == "warn"  && (loggingType == "warn")) {
	} else { loggingText = null }
	if (loggingText) {
		log."${loggingType}" loggingText
		if (internalLogging) {
			if (state.logger.size() >= internalLoggingSize) { state.logger.pop() }
			state.logger.push("<b>log.${loggingType}:</b>\t${loggingText}")
		}
	}
}
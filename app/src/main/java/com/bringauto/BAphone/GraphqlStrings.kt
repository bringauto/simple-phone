package com.bringauto.BAphone

private const val loginString = "query UserLogin {\nUserQuery {\nlogin(login: {password: \"%password%\", " +
    "userName: \"%username%\"}) {\nemail\nroles\ntenants{nodes{id name}}\n}\n}\n}\n"

private const val carGetString = "query Q{\nCarQuery {\ncars {\nnodes {\nname\nid\nlatitude\nlongitude\n" +
    "status\nfuel\nhwId\ncompanyName\ncarAdminPhone\ncallTwiml\nunderTest\nspeed\nrouteId\n}\n}\n}\n}"

private const val carUpdateString = "mutation UpdateCar{\nCarMutation {\nupdateCar(car: {\nid: %id%,\nname: " +
    "\"%name%\",\nrequireNewToken: false,\nstatus: %status%,\nbutton: NORMAL,\nunderTest: %underTest%," +
    "\nrouteId: %routeId%,\n}){\nid\n}\n}\n}"

fun getLoginString(username: String, password: String): String {
    var returnString = loginString
    returnString = returnString.replace("%password%", password)
    returnString = returnString.replace("%username%", username)
    return returnString
}

fun getCarGetString(): String {
    return carGetString
}

fun getCarUpdateString(carId: Int, carName: String, carStatus: String, carUnderTest: Boolean, routeId: Int): String {
    var returnString = carUpdateString
    returnString = returnString.replace("%id%", carId.toString())
    returnString = returnString.replace("%name%", carName)
    returnString = returnString.replace("%status%", carStatus)
    returnString = returnString.replace("%underTest%", carUnderTest.toString())
    returnString = returnString.replace("%routeId%", routeId.toString())
    return returnString
}
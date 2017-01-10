package com.sapient.parkingAssistant.actors

import akka.actor.{Actor, ActorLogging}
import com.sapient.parkingAssistant.actors.SecurityMonitor.SecurityCheck
import com.sapient.parkingAssistant.domain.{Vehicle, VehicleStatus}
import com.sapient.parkingAssistant.kafkaConnectors.MessageSender

/**
  * Created by agu225 on 9/1/2017.
  */
object SecurityMonitor {
  case class SecurityCheck(vehicle : Vehicle)
}

class SecurityMonitor extends Actor with ActorLogging  {
  val blacklistedVehicleList = List("222","333","444")
  val securityCheckSender = new MessageSender
  def receive = {
    case SecurityCheck(vehicle : Vehicle) => {
      log info (s"Security Check request received for Vehicle: ${vehicle.regNumber}")
      if(blacklistedVehicleList.contains(vehicle.regNumber)){
        val securityStatus:VehicleStatus = VehicleStatus(vehicle,false,"Security Check Failed")
        securityStatus
        //securityCheckSender.publishMessage("ResponseTopic",(new Gson()).toJson(securityStatus))
      }else{
        val securityStatus:VehicleStatus = VehicleStatus(vehicle,true,"Security OK")
        println("Securtiy passed!")
        sender ! securityStatus
        //securityCheckSender.publishMessage("ResponseTopic",(new Gson).toJson(securityStatus))
      }
    }
  }
}


package com.sapient.parkingAssistant.actors

import akka.actor.{Actor, ActorLogging}
import com.sapient.parkingAssistant.actors.SlotMonitor.RequestSlot
import com.sapient.parkingAssistant.domain.{ParkingSlot, Vehicle, VehicleStatus}

import scala.util.control.Breaks

/**
  * Created by agu225 on 10/1/2017.
  */
class FloorMonitor(floorNumber: String) extends Actor with ActorLogging {
  val slotMap = scala.collection.mutable.Map[Int, Boolean]((1 -> false), (2 -> false), (3 -> false), (4 -> false), (5 -> false))
  val loop = new Breaks

  override def receive = {
    case AllocateParkingSlot(vehicle) => {
      loop.breakable {
        for ((slot, status) <- slotMap) {
          if (!status) {
            val parkingSlot = new ParkingSlot(floorNumber, slot, status)
            sender ! parkingSlot
            loop.break
          }
        }
      }
    }

    case ParkTheCar(vehicle, parkingSlot) => {

      val currentSlotStatus = slotMap.get(parkingSlot.slotNumber)
      if (!currentSlotStatus.get) {
        slotMap.put(parkingSlot.slotNumber, true)
        println(s"$vehicle parked at  [ ${parkingSlot.floorNumber} , ${parkingSlot.slotNumber} ] ")
        sender ! VehicleStatus(vehicle, true, s"$vehicle parked at  [ ${parkingSlot.floorNumber} , ${parkingSlot.slotNumber} ] ")
      } else {
        println("Initiating a new request")
        ParkingController.slotActor ! RequestSlot(vehicle)
      }
    }
  }
}

case class ParkTheCar(vehicle: Vehicle, parkingSlot: ParkingSlot)

case class AllocateParkingSlot(vehicle: Vehicle)
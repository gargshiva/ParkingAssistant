package com.sapient.parkingAssistant.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.sapient.parkingAssistant.actors.SlotMonitor.RequestSlot
import com.sapient.parkingAssistant.domain.{ParkingSlot, Vehicle}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

/**
  * Created by agu225 on 9/1/2017.
  */

object SlotMonitor {
  case class RequestSlot(vehicle: Vehicle)
  /*println("Scala working")
  val system = ActorSystem("parkinglot")
  val router: ActorRef = system.actorOf(FromConfig.props(), "scatterGatherRouter")
  val parkingSystem: ActorRef = system.actorOf(Props(new SlotMonitor(router)), "SlotMonitor")

  for(a <- 1 to 10 ){
    parkingSystem ! RequestSlot(new Vehicle())
  }
*/
}

class SlotMonitor(router: ActorRef) extends Actor with ActorLogging {
  implicit val timeout = Timeout(10 seconds)
  val floor1: ActorRef = context.actorOf(Props(new FloorMonitor("floor1")), "floor1")
  val floor2: ActorRef = context.actorOf(Props(new FloorMonitor("floor2")), "floor2")

  override def receive = {
    case RequestSlot(vehicle) => {
      val parkingSlot: Future[Any] = router ? AllocateParkingSlot(vehicle)
      val res = Await.result(parkingSlot, timeout.duration)
      // Got the free slot
      println(s"Got the free slot =>>>>>>>>>>>>>>>> $res")
      val parkingS = res.asInstanceOf[ParkingSlot]
      val floorNumber = parkingS.floorNumber
      if (floorNumber.equals("floor2")) {
        floor2 ! ParkTheCar(vehicle, parkingS)

      } else {
        floor1 ! ParkTheCar(vehicle, parkingS)
      }

    }

    case randomInput => println(s"Unknown Inputs $randomInput")
  }
}


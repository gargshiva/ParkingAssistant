package com.sapient.parkingAssistant.actors

import java.util
import java.util.Properties

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.routing.FromConfig
import akka.util.Timeout
import com.sapient.parkingAssistant.actors.ParkingController._
import com.sapient.parkingAssistant.actors.SecurityMonitor.SecurityCheck
import com.sapient.parkingAssistant.actors.SlotMonitor.RequestSlot
import com.sapient.parkingAssistant.domain.{Vehicle, VehicleStatus}
import kafka.consumer.ConsumerConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong

/**
  * Created by agu225 on 9/1/2017.
  */
object ParkingController {
  implicit val timeout = Timeout(5 seconds)
  val system = ActorSystem("ParkingAssistant")
  val router = system.actorOf(FromConfig.props(), "scatterGatherRouter")
  val slotActor = system.actorOf(Props(new SlotMonitor(router)), "SlotMonitor")
  val securityActor = system.actorOf(Props[SecurityMonitor], "securityActor")

  private def createConsumerConfig(zookeeper: String, groupId: String): ConsumerConfig
  = {
    val props = new Properties()
    props.put("zookeeper.connect", zookeeper)
    props.put("group.id", groupId)
    props.put("auto.offset.reset", "smallest")
    props.put("zookeeper.session.timeout.ms", "500")
    props.put("zookeeper.sync.time.ms", "250")
    props.put("auto.commit.interval.ms", "1000")
    new ConsumerConfig(props)
  }

}

class ParkingController(zookeeper: String, groupId: String, val topic: String) {
  //private val consumer = kafka.consumer.Consumer.createJavaConsumerConnector(createConsumerConfig(zookeeper,groupId))
  def startVehicleConsumer(): Unit = {
    val topicMap = new util.HashMap[String, Integer]()
    topicMap.put(topic, 1)
    //val consumerStreamsMap = consumer.createMessageStreams(topicMap)
    //val streamList = consumerStreamsMap.get(topic)
    //for (stream <- streamList; aStream <- stream) {
    // val jsonVehicle = new String(aStream.message())
    //println("Vehicle arrived for parking :: " + jsonVehicle )

    // val vehicle = (new Gson).fromJson(jsonVehicle,Vehicle)
    val vehicle = new Vehicle("AAA")
    println("Created Vehicle")
    val futureSlotStatus = slotActor ? RequestSlot(vehicle)
    val futureSecurityStatus = securityActor ? SecurityCheck(vehicle)

    val parkingStatusFuture = for {
      slotStatusResult <- futureSlotStatus
      securityStatusResult <- futureSecurityStatus

    } yield (slotStatusResult.asInstanceOf[VehicleStatus]).flag

    parkingStatusFuture onComplete {
      case status =>
        if (status.get == (true)) {
          //post to Kafka
          println("Everything OK!")
        }
        else {
          println("Everything NOT OK!")
          //post to kafka if slot fails
          //post to kafka and release booked slot if seurity fails
          //if (slotStatusResult.asInstanceOf[VehicleStatus]).flag)
        }

    }
  }

  /*if (consumer != null) {
      consumer.shutdown()
    }*/
  //}
  /*override def receive = {
    case _ => println("Someone banged ParkingController")
  }*/
}

object Myapp {
  def main(args: Array[String]): Unit = {
    //Testing
    println("Testing Started")
    val parkingController = new ParkingController("localhost:2181", "group1", "ParkVehicle")
    parkingController.startVehicleConsumer()
  }
}

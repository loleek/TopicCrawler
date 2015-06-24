package core

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.io.IO
import spray.can.Http
import weibo.WeiboMessages.Start
import com.typesafe.config.ConfigFactory

object Main {
  def main(args: Array[String]): Unit = {
    args(0) match {
      case "master" => {
        implicit val system = ActorSystem("tc-master-system",ConfigFactory.load())

        val service = system.actorOf(Props[TopicCrawlerHttpService], "tc-service")
        val master = system.actorOf(Props[MasterBackend], "tc-master")

        IO(Http) ! Http.Bind(service, "localhost", port = 12306)
      }
      case "slave" => {
        val system = ActorSystem("tc-slave-system")

        val slave = system.actorOf(Props[SlaveBackend], "tc-slave")
        slave ! Start
      }
      case "test" => {
        implicit val system = ActorSystem("localsystem")

        val service = system.actorOf(Props[TopicCrawlerHttpService], "tc-service")
        val master = system.actorOf(Props[MasterBackend], "tc-master")

        val slave = system.actorOf(Props[SlaveBackend], "tc-slave")
        slave ! Start

        IO(Http) ! Http.Bind(service, "localhost", port = 12306)
      }
    }
  }
}
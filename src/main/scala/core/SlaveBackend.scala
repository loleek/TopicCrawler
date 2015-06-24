package core

import java.net.InetAddress
import akka.actor.Actor
import weibo.WeiboMessages._
import akka.actor.Props
import weibo.Spider
import akka.actor.PoisonPill
import akka.actor.ActorLogging
/**
 * Created by dk on 2015/6/16.
 */
class SlaveBackend extends Actor with ActorLogging {

  val master = context.actorSelection("akka.tcp://tc-master-system@49.122.47.30:5150/user/tc-master")
  //  val master = context.actorSelection("akka://localsystem/user/tc-master")
  val hostname = InetAddress.getLocalHost.getHostName

  val spider = context.actorOf(Props[Spider], "spider")

  var currentTask: SpiderTask = null

  def receive = {
    case Start => master ! Register(hostname)
    case Shutdown(reason) => {
      master ! Unregister(hostname)
      context.system.shutdown()
    }
    case task @ SpiderTask(_, topic) => {
      println(task)
      currentTask = task
      spider ! topic
    }
    case info @ MasterInfo(_) => {
      spider ! info
    }
    case TaskFinished(_, _, content) => {
      master ! TaskFinished(currentTask.taskid, currentTask.topic, content)
      currentTask = null
    }

    case TaskFailed(_, _) => {
      master ! TaskFailed(currentTask.taskid, currentTask.topic)
    }
  }
}

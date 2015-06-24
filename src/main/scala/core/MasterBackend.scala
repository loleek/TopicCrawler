package core

import scala.collection.mutable

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import weibo.DataRecorder
import weibo.WeiboAccountManager
import weibo.WeiboMessages.MasterInfo
import weibo.WeiboMessages.Register
import weibo.WeiboMessages.SpiderTask
import weibo.WeiboMessages.TaskFailed
import weibo.WeiboMessages.TaskFinished
import weibo.WeiboMessages.Unregister

/**
 * Created by dk on 2015/6/16.
 */
class MasterBackend extends Actor with ActorLogging {

  implicit val order = TaskOrdering

  val atm = context.actorOf(Props[WeiboAccountManager], "accountmanager")
  val da = context.actorOf(Props[DataRecorder], "datarecorder")

  val hosts = new mutable.HashMap[String, ActorRef]()
  val taskqueue = new mutable.PriorityQueue[SpiderTask]()
  val workhost = new mutable.HashMap[Long, String]()
  val idlehost = new mutable.Queue[String]()

  var resultMap = new mutable.HashMap[Long, Result]()

  def receive = {
    case Register(hostname) => {
      hosts += (hostname -> sender)
      idlehost.enqueue(hostname)
      sender ! MasterInfo(atm)
    }
    case Unregister(hostname) => {
      hosts -= (hostname)
      idlehost.dequeueAll { _ == hostname }
      if (hosts.isEmpty)
        context.system.shutdown()
    }
    case task @ SpiderTask(taskid, topic) => {
      if (idlehost.isEmpty) {
        taskqueue.enqueue(task)
      } else {
        val hostname = idlehost.dequeue()
        workhost += (taskid -> hostname)
        hosts(hostname) ! task
      }
    }
    case result @ TaskFinished(taskid, topic, content) => {
      val hostname = workhost(taskid)
      if (hosts.contains(hostname))
        idlehost.enqueue(hostname)
      workhost -= taskid
      if (!taskqueue.isEmpty)
        self ! taskqueue.dequeue()

      da ! result

      resultMap += (taskid -> Result(taskid, 2, "finish", content.getOrElse("").split("\n").toList))
      
      clearResultMap()
    }
    case TaskFailed(taskid, topic) => {
      val hostname = workhost(taskid)
      if (hosts.contains(hostname))
        idlehost.enqueue(hostname)
      workhost -= taskid
      if (!taskqueue.isEmpty)
        self ! taskqueue.dequeue()

      resultMap += (taskid -> Result(taskid, -1, "failed", List("")))
      
      clearResultMap()
    }
    case TaskQuery(taskid) => {
      if (resultMap.contains(taskid)) {
        sender ! resultMap(taskid)
      } else {
        if (workhost.contains(taskid))
          sender ! Result(taskid, 1, "crawling", List(""))
        else {
          sender ! Result(taskid, 0, "waiting for spider", List(""))
        }
      }
    }
  }

  def clearResultMap() {
    val curremtTime = System.currentTimeMillis()
    resultMap = resultMap.filter { pair =>
      (curremtTime - pair._1) < (1 * 60 * 60 * 1000)
    }
  }
}

object TaskOrdering extends Ordering[SpiderTask] {
  def compare(a: SpiderTask, b: SpiderTask) = a.taskid compare b.taskid
}

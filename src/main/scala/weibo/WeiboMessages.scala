package weibo

import akka.actor.ActorRef
import weibo.WeiboModel.Cursor

/**
 * Created by dk on 2015/6/16.
 */
object WeiboMessages {

  case class Register(hostname: String)

  case class Unregister(hostname: String)

  case class MasterInfo(atm: ActorRef)

  case class TopicPageContent(topic: String, content: String)

  case class TopicInfo(topic: String, contaiderid: String, appid: String, uid: String)

  case class NoneTopicInfo(reason: String)

  case class TopicWeiboPage(content: String)

  case class TopicWeiboResult(maxPage: Option[Int], next_cursor: Cursor, weibos: String)

  case object TopicWeiboParseFailed

  case object Start

  case class SpiderTask(taskid: Long, topic: String)

  case class TaskFinished(taskid: Long, topic: String, content: Option[String])

  case object StartTask

  case class TaskFailed(taskid: Long, topic: String)

  case class Shutdown(reason: String)

}

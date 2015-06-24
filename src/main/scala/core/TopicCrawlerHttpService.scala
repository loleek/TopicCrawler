package core

import scala.collection.mutable
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import spray.httpx.marshalling._
import spray.json.DefaultJsonProtocol
import spray.json.JsonFormat
import spray.routing.HttpServiceActor
import spray.http.MediaTypes
import weibo.WeiboMessages.SpiderTask

case class Result(taskid: Long, status: Int, reason: String, text: List[String])
case class TaskQuery(taskid: Long)

object TopicCrawlerProtocol extends DefaultJsonProtocol {
  implicit val TaskFormat = jsonFormat2(SpiderTask)
  implicit val ResultFormat = jsonFormat4(Result)
}

class TopicCrawlerHttpService extends HttpServiceActor {

  import TopicCrawlerProtocol._
  import spray.httpx.SprayJsonSupport._
  import spray.util._
  import scala.concurrent.ExecutionContext.Implicits.global

  val masterRef = context.actorSelection("/user/tc-master")

  implicit val timeout = Timeout(5 seconds)

  def receive = runRoute {
    path("topic" / Segment) { topic =>
      val task = SpiderTask(System.currentTimeMillis(), topic)
      masterRef ! task

      respondWithMediaType(MediaTypes.`application/json`) { ctx =>
        ctx.complete(marshal(task))
      }
    } ~
      path("task" / LongNumber) { taskid =>
        val future = masterRef ? TaskQuery(taskid)
        respondWithMediaType(MediaTypes.`application/json`) { ctx =>
          future.map {
            case result: Result => ctx.complete(marshal(result))
          }
        }
      }
  }
}
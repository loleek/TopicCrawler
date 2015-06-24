package weibo

import scala.collection.mutable.ArrayBuffer

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import com.fasterxml.jackson.core.JsonParseException

import akka.actor.Actor
import weibo.WeiboMessages._
import weibo.WeiboModel._

/**
 * Created by dk on 2015/6/16.
 */
class Parser extends Actor {
  val regex = """containerid=([0-9a-zA-Z]+)__timeline__mobile_info_-_pageapp%3A([0-9a-zA-Z]+)&uid=([0-9]+)""".r
  val failregex = """containerid=([0-9a-zA-Z]+)""".r

  def receive = {
    case TopicPageContent(topic, content) => {
      val result = regex.findFirstIn(content).getOrElse("")
      result match {
        case regex(cid, appid, uid) => sender ! TopicInfo(topic, cid, appid, uid)
        case _ => {
          failregex.findFirstIn(content) match {
            case Some(s) => sender ! NoneTopicInfo("empty topic")
            case None    => sender ! NoneTopicInfo("account exception")
          }
        }
      }
    }
    case TopicWeiboPage(content) => {
      try {
        implicit val formats = DefaultFormats
        val json = parse(content)
        val page = json.extract[TopicPage]
        var result = ArrayBuffer.empty[String]
        for {
          card <- page.cards
          group <- card.card_group
          weibo <- group
        } result += WeiboToString(weibo.mblog)

        val cursorJson = page.next_cursor.getOrElse("{}")
        val cursor = parse(cursorJson).extract[Cursor]

        sender ! TopicWeiboResult(page.maxPage, cursor, result.mkString("\n"))
      } catch {
        case _: JsonParseException => {
          sender ! TopicWeiboParseFailed
        }
      }
    }
  }

  def WeiboToString(weibo: Weibo): String = {
    val mid = weibo.mid
    val text = weibo.text
    val uid = weibo.user.id
    val name = weibo.user.screen_name
    val source = weibo.source
    val time = weibo.created_at
    val rid = weibo.retweeted_status match {
      case Some(retweet) => retweet.mid
      case None          => "null"
    }
    val pid = weibo.pid.getOrElse("null")
    val reposts_count = weibo.reposts_count
    val comments_count = weibo.comments_count
    val attitudes_count = weibo.attitudes_count
    val pic = weibo.pic_ids.mkString("[", ",", "]")
    val topic = weibo.topic_struct.map(_.topic_title).mkString(" ")
    s"${mid}\t${text}\t${uid}\t${name}\t${source}\t${time}\t${rid}\t${pid}\t${reposts_count}\t${comments_count}\t${attitudes_count}\t${pic}\t${topic}"
  }
}

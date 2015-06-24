package weibo

import java.net.InetAddress

import scala.collection.mutable.ArrayBuffer

import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import weibo.WeiboAccountManager.NoneWeiboAccount
import weibo.WeiboAccountManager.WeiboAccount
import weibo.WeiboAccountManager.WeiboAccountRequest
import weibo.WeiboAccountManager.WrongWeiboAccount
import weibo.WeiboMessages.MasterInfo
import weibo.WeiboMessages.NoneTopicInfo
import weibo.WeiboMessages.Shutdown
import weibo.WeiboMessages.TaskFailed
import weibo.WeiboMessages.TaskFinished
import weibo.WeiboMessages.TopicInfo
import weibo.WeiboMessages.TopicPageContent
import weibo.WeiboMessages.TopicWeiboPage
import weibo.WeiboMessages.TopicWeiboParseFailed
import weibo.WeiboMessages.TopicWeiboResult
import weibo.WeiboUtil.getContent
import weibo.WeiboUtil.login

/**
 * Created by dk on 2015/6/16.
 */
class Spider extends Actor with ActorLogging {

  var client: CloseableHttpClient = null
  var currentAccount: WeiboAccount = null

  val hostname = InetAddress.getLocalHost.getHostName

  var atm: ActorRef = null
  val parser = context.actorOf(Props[Parser], "parser")

  var topic = ""
  var topicInfo: TopicInfo = null
  var result = ArrayBuffer.empty[String]
  var page = 1
  var maxPage = 20
  var next_cursor: Option[String] = None
  var period = 0

  def receive = {
    case MasterInfo(atm) => {
      this.atm = atm

      atm ! WeiboAccountRequest(hostname)
    }
    case topic: String => {
      this.topic = topic
      period = 1
      val topicurl = "http://m.weibo.cn/k/" + topic + "?from=feed"
      val content = getContent(client, topicurl)
      parser ! TopicPageContent(topic, content)
    }
    case info @ TopicInfo(topic, cid, appid, uid) => {
      topicInfo = info
      period = 2
      val disurl = s"http://m.weibo.cn/page/pageJson?containerid=&containerid=${cid}__timeline__mobile_info_-_pageapp%3A${appid}&uid=${uid}&v_p=11&ext=&fid=${cid}__timeline__mobile_info_-_pageapp%3A${appid}&uicode=10000011"
      val content = getContent(client, disurl)
      parser ! TopicWeiboPage(content)
    }
    case TopicWeiboResult(maxPage, cursor, weibos) => {
      page match {
        case 1 => {
          this.maxPage = maxPage match {
            case Some(count) => if (count < 20) count else 20
            case None        => 20
          }

          this.next_cursor = for {
            lsi <- cursor.last_since_id
            rt <- cursor.res_type
            nsi <- cursor.next_since_id
          } yield {
            s"&next_cursor=%7B%22last_since_id%22:${lsi},%22res_type%22:${rt},%22next_since_id%22:${nsi}%7D"
          }

          result += weibos

          page = page + 1

          if (page <= this.maxPage) {

            val cur = next_cursor match {
              case Some(c) => c
              case None    => ""
            }
            val disurl = s"http://m.weibo.cn/page/pageJson?containerid=&containerid=${topicInfo.contaiderid}__timeline__mobile_info_-_pageapp%3A${topicInfo.appid}&uid=${topicInfo.uid}&v_p=11&ext=&fid=${topicInfo.contaiderid}__timeline__mobile_info_-_pageapp%3A${topicInfo.appid}&uicode=10000011${cur}&page=${this.page}"
            val content = getContent(client, disurl)
            parser ! TopicWeiboPage(content)
          } else {
            context.parent ! TaskFinished(0L, topic, Some((result.mkString("\n"))))
            topic = ""
            topicInfo = null
            result = ArrayBuffer.empty[String]
            page = 1
            this.maxPage = 20
            this.next_cursor = None
            period = 0
          }

        }
        case num if (num < this.maxPage) => {
          result += weibos
          
          page = page + 1
          
          this.next_cursor = for {
            lsi <- cursor.last_since_id
            rt <- cursor.res_type
            nsi <- cursor.next_since_id
          } yield {
            s"&next_cursor=%7B%22last_since_id%22:${lsi},%22res_type%22:${rt},%22next_since_id%22:${nsi}%7D"
          }

          val cur = next_cursor match {
            case Some(c) => "&" + c
            case None    => ""
          }
          val disurl = s"http://m.weibo.cn/page/pageJson?containerid=&containerid=${topicInfo.contaiderid}__timeline__mobile_info_-_pageapp%3A${topicInfo.appid}&uid=${topicInfo.uid}&v_p=11&ext=&fid=${topicInfo.contaiderid}__timeline__mobile_info_-_pageapp%3A${topicInfo.appid}&uicode=10000011${cur}&page=${this.page}"
          val content = getContent(client, disurl)
          parser ! TopicWeiboPage(content)
        }
        case _ => {
          result += weibos

          context.parent ! TaskFinished(0L, topic, Some((result.mkString("\n"))))
          topic = ""
          topicInfo = null
          result = ArrayBuffer.empty[String]
          page = 1
          this.maxPage = 20
          this.next_cursor = None
          period = 0
        }
      }
    }
    case NoneTopicInfo(reason) => {
      reason match {
        case "empty topic" => {
          context.parent ! TaskFinished(0L, topic, None)
          topic = ""
          topicInfo = null
          result = ArrayBuffer.empty[String]
          page = 1
          maxPage = 20
          next_cursor = None
          period = 0
        }
        case "account exception" => {
          atm ! WrongWeiboAccount(hostname)
          this.currentAccount = null
        }
      }
    }
    case TopicWeiboParseFailed => {
      atm ! WrongWeiboAccount(hostname)
      this.currentAccount = null
    }
    case account @ WeiboAccount(name, pass) => {
      period match {
        case 0 => {
          this.currentAccount = account
          client = HttpClients.createDefault()
          try {
            login(client, account)
          } catch {
            case _: Exception => {
              atm ! WrongWeiboAccount(hostname)
              this.currentAccount = null
            }
          }
        }
        case 1 => {
          Thread.sleep(3 * 60 * 1000)

          this.currentAccount = account
          client = HttpClients.createDefault()
          try {
            login(client, account)

            val topicurl = "http://m.weibo.cn/k/" + topic + "?from=feed"
            val content = getContent(client, topicurl)
            parser ! TopicPageContent(topic, content)
          } catch {
            case _: Exception => {
              atm ! WrongWeiboAccount(hostname)
              this.currentAccount = null
            }
          }
        }
        case 2 => {
          Thread.sleep(3 * 60 * 1000)

          this.currentAccount = account
          client = HttpClients.createDefault()
          try {
            login(client, account)

            val cur = next_cursor match {
              case Some(c) => "&" + c
              case None    => ""
            }
            val disurl = s"http://m.weibo.cn/page/pageJson?containerid=&containerid=${topicInfo.contaiderid}__timeline__mobile_info_-_pageapp%3A${topicInfo.appid}&uid=${topicInfo.uid}&v_p=11&ext=&fid=${topicInfo.contaiderid}__timeline__mobile_info_-_pageapp%3A${topicInfo.appid}&uicode=10000011${cur}&page=${this.page}"
            val content = getContent(client, disurl)
            parser ! TopicWeiboPage(content)
          } catch {
            case _: Exception => {
              atm ! WrongWeiboAccount(hostname)
              this.currentAccount = null
            }
          }
        }
      }
    }
    case NoneWeiboAccount => {
      period match {
        case 0 => context.parent ! Shutdown("no acccount")
        case _ => {
          context.parent ! TaskFailed(0L, topic)
          context.parent ! Shutdown("no account")
        }
      }
    }
  }
}
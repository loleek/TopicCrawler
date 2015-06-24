package weibo

object WeiboModel {
  case class TopicPage(count: Option[Int], maxPage: Option[Int], cards: Option[Card], next_cursor: Option[String])
  case class Card(card_group: Option[List[Cardgroup]])
  case class Cardgroup(mblog: Weibo)
  case class Weibo(created_at: String, mid: String, text: String, source: String, pic_ids: List[String], user: User, pid: Option[String], retweeted_status: Option[ReWeibo], reposts_count: Int, comments_count: Int, attitudes_count: Int, topic_struct: List[Topic])
  case class ReWeibo(mid: String)
  case class User(id: String, screen_name: String)
  case class Topic(topic_title: String)
  case class Cursor(last_since_id: Option[String], res_type: Option[String], next_since_id: Option[String])
}
# TopicCrawler
This a distributed-topic-crawler for Sina Weibo , which based on scala ,akka,json4s and spray.
Akka for actor-model.Master is responsible for account management and task disturbing.Slave is responsible for data-crawling 
and data-extracting.
Json4s for ORM,mapping the json data to Scala object.
Spray for Restful interface.
(
hostname:port/topic/topicName --> submit a topic crawling task -->response format {taskid:Long,topic:String}
hostname:port/task/taskid -->query the stats of a task -->response format {taskid:Long,stat:Int,reason:String,topicweibotext:String}
)

package weibo

import akka.actor.ActorLogging
import akka.actor.Actor
import weibo.WeiboMessages.TaskFinished
import java.io.File
import java.io.PrintWriter

class DataRecorder extends Actor {
  
  val folder="topics"
  
  def receive = {
    case TaskFinished(taskid, topic, content)=>{
      val file=new File(folder+File.separatorChar+s"${topic}-${taskid}.txt")
      val out=new PrintWriter(file)
      out.println(content)
      out.flush()
      out.close()
    }
  }
}


package cp.serverPr

import scala.collection.mutable.Queue
import java.util.concurrent.atomic._


class ServerState() {

  private val maxCmds : Int = 4
  
  // number of total commands run by the server
  var total_cmds = new AtomicInteger(0)
  // number of currently active commands
  var cmds = new AtomicInteger(0)

  //queue of command history
  val tasks = Queue[String]()
  //queue of command result history
  val outputs = Queue[String]()


  //methods that deal with the number of total commands  
  def incrTotalCmds() : Int = {
    total_cmds.incrementAndGet()
  }

  def getTotalCmds() : Int = {
    total_cmds.get()
  }

  //methods that save the history

  def addResult(result : String) : Unit = outputs.synchronized {
    outputs.enqueue(result)
  }

  def saveTask(cmd : String) : Unit = tasks.synchronized {
    tasks.enqueue(cmd)
  }

  //methods that deal with active commands

  def releaseCmd(): Int = {
    cmds.decrementAndGet()
  }

  def incrCmd(): Int = {
    cmds.incrementAndGet()
  }

  def canRunCmd(cmd : Int): Boolean = {
    (cmd < maxCmds)
  }

  def getNumCmds(): Int = {
    cmds.get
  }

  def toHtml: String = {
    s"""
    <p><strong>total_cmds:</strong> ${total_cmds.get()} </p>
    <h6>Command history</h6>
    |  <ul>
    |    ${tasks.map(item => s"<li>$item</li>").mkString("\n")}
    |  </ul>
    |<h6>Result history</h6>
    |  <ul>
    |    ${outputs.map(item => s"<li>$item</li>").mkString("\n")}
    |  </ul>
    """.stripMargin
  }
    //s"<p><strong>total_cmds:</strong> ${total_cmds.get()}  /// number of cmds:  ${getNumCmds}</p>"
    

}

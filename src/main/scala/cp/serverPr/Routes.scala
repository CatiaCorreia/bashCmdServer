package cp.serverPr

import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import org.slf4j.LoggerFactory
import scala.sys.process._
import java.time.Instant


object Routes {
  private val logger = LoggerFactory.getLogger(getClass)
  private val state = new ServerState()

  val routes: IO[HttpRoutes[IO]] =
   IO{HttpRoutes.of[IO] {

     // React to a "status" request
     case GET -> Root / "status" =>
       Ok(state.toHtml)
         .map(addCORSHeaders)
         .map(_.withContentType(org.http4s.headers.`Content-Type`(MediaType.text.html)))


     // React to a "run-process" request
     case req@GET -> Root / "run-process" =>
       val cmdOpt = req.uri.query.params.get("cmd")
       val userIp = req.remoteAddr.getOrElse("unknown")

       //// printing to the terminal instead of a logging file
       //println(">>> got run-process!")
       //println(s">>> Cmd: ${cmdOpt}")
       //println(s">>> userIP: $userIp")

       cmdOpt match {
        case Some(cmd) =>
        {
          val cmds : Array[String] = cmd.split(";")

          var result : String = ""

          for (c <- cmds) 
          {
            state.saveTask(c)
            var allowed = false

            while(!allowed)
            {
              val t = state.getNumCmds
              if(state.canRunCmd(t) && state.cmds.compareAndSet(t, t + 1)) 
              {
                allowed = true
              }
            } 
          
            if (c=="stopServer") state.stopServer()

            //if(!state.continue_running) Thread.interrupt
            val output = runProcess(c, userIp.toString)
            state.releaseCmd
            result = result + output
          
          }    

          Ok(result)
          .map(addCORSHeaders)
        }

      case None =>
        BadRequest("⚠️ Command not provided. Use /run-process?cmd=<your_command>")
          .map(addCORSHeaders)
      }
    }
  }

  /** Run a given process and adds its output to the outputs queue. */
  private def runProcess(cmd: String, userIp: String): String = {
    logger.info(s"🔹 Starting process (${state.getTotalCmds}) for user $userIp: $cmd")

    val result : String = Thread.currentThread.getName + " for " + cmd + ": " + cmd.trim.!! + "\n"
    val tComands = state.incrTotalCmds
    state.addResult(cmd + " finished at " + Instant.now() + " in thread " + Thread.currentThread.getName + " as the comand number " + tComands)
    result
  }

  /** Add extra headers, required by the client. */
  def addCORSHeaders(response: Response[IO]): Response[IO] = {
    response.putHeaders(
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Content-Type, Authorization",
      "Access-Control-Allow-Credentials" -> "true"
    )
  }
}



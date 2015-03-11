package com.xebialabs.jello

import java.util.Date

import com.xebialabs.jello.domain.tickets.RangeConverter
import com.xebialabs.jello.domain.{Jira, Trello}
import com.xebialabs.jello.watch.DragWatcherActor
import com.xebialabs.jello.watch.DragWatcherActor.Tick

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

object Main extends App with RangeConverter {

  implicit val jira = new Jira()
  val trello = new Trello()
  val jello = new Jello(jira, trello)


  println("Welcome to Jello!")

  println("Enter tickets range:")
  val ticketsForEstimation = inputToTickets(StdIn.readLine())

  val defaultTitle = s"Estimation session: ${new Date().toString}"
  println(s"Enter board title [$defaultTitle]:")
  val title = StdIn.readLine() match {
    case "" | null => defaultTitle
    case t => t
  }

  ticketsForEstimation.flatMap {
    tickets =>
      val ids: Seq[String] = tickets.map(_.id)
      jello.prepareForEstimation(ids, title)
  } flatMap { board =>
    Future {
      println(s"Please do the estimations at: ${board.shortUrl} and press enter when you're done.")

      val token = system.scheduler.schedule(1 second, 1 second, system.actorOf(DragWatcherActor.props(board)), Tick)
      StdIn.readLine()
      token

    } flatMap { token =>
      token.cancel()
      jello.saveEstimationsFrom(board)
    } flatMap { nothing =>
      trello.archiveBoard(board.id)
    } andThen {
      case r =>
        println("Thanks for using Jello. Have a nice day.")
        system.shutdown()
    }
  }


}

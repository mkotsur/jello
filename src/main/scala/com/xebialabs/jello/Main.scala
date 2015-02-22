package com.xebialabs.jello

import com.xebialabs.jello.domain.{Jira, Trello}
import com.xebialabs.jello.watch.DragWatcherActor
import com.xebialabs.jello.watch.DragWatcherActor.Tick

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn

object Main extends App {

  val jira = new Jira()
  val trello = new Trello()
  val jello = new Jello(jira, trello)


  println("Welcome to Jello!")

  println("Enter board id:")
  val rapidBoardId = StdIn.readLine().trim.toInt

  println("Enter ticket to start with:")
  val rangeBegin = StdIn.readLine()

  println("Enter ticket to end with:")
  val rangeEnd = StdIn.readLine()

  jira.getRapidBoardTickets(rapidBoardId, rangeBegin, rangeEnd).flatMap {
    tickets => jello.prepareForEstimation(tickets.map(_.id))
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
      println("Thanks for using Jello. Have a nice day.")
      trello.archiveBoard(board.id)
    } andThen {
      case r =>
        println("Thanks for using Jello. Have a nice day.")
        system.shutdown()
    }
  }


}

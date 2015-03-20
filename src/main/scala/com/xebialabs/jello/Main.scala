package com.xebialabs.jello

import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.jello.domain.tickets.RangeConverter
import com.xebialabs.jello.domain.{Jira, Trello}
import com.xebialabs.jello.watch.DragWatcherActor
import com.xebialabs.jello.watch.DragWatcherActor.Tick

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Success, Failure}

object Main extends App with RangeConverter with LazyLogging {

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

  val isDone = ticketsForEstimation.flatMap { tickets =>
      jello.prepareForEstimation(tickets.map(_.id), title)
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
    }
  }

  isDone.andThen {
    case Failure(e) =>
      logger.warn("There was an error during the execution: ", e)
    case Success(_) =>
      logger.info("Thanks for using Jello. Have a nice day.")
  } andThen {
    case _ =>
      logger.debug("Shutting down the actor system.")
      system.shutdown()
  }

  system.awaitTermination()

}

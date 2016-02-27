package com.xebialabs.jello

import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.jello.conf.DefaultConfig
import com.xebialabs.jello.domain.tickets.RangeConverter
import com.xebialabs.jello.domain.{Jira, Trello}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Main extends App with RangeConverter with DefaultConfig with LazyLogging {

  implicit val jira = new Jira() with DefaultConfig
  val trello = new Trello() with DefaultConfig
  val jello = new Jello(jira, trello) with DefaultConfig

  println("Welcome to Jello!")

  val defaultTitle = s"Estimation session: ${new Date().toString}"

  val estimation = for (
    (_, tickets) <- jello.validateTrelloPermissions() zip jello.getTicketsForEstimation;
    title <- askForInput(s"Enter board title [$defaultTitle]:").map(_.getOrElse(defaultTitle));
    board <- jello.prepareForEstimation(Future(tickets), title);
    _ <- jello.handleEstimation(board);
    _ <- jello.saveEstimationsFrom(board);
    _ <- trello.closeBoard(board.id)
  ) yield ()

  
  estimation.andThen {
    case Failure(e) =>
      logger.warn("There was an error during the execution: ", e)
      die(1)
    case Success(_) =>
      logger.info("Thanks for using Jello. Have a nice day.")
      die(0)
  }


  def die(code: Int = 0): Unit = {
    logger.debug("Shutting down the actor system.")
    system.shutdown()
    system.awaitTermination()
    logger.debug(s"Exiting with code $code.")
    System.exit(code)
  }
}

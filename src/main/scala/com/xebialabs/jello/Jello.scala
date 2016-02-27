package com.xebialabs.jello

import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.jello.Main._
import com.xebialabs.jello.conf.ConfigAware
import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.Trello.Board
import com.xebialabs.jello.domain.{Jira, Trello}
import com.xebialabs.jello.watch.DragWatcherActor
import com.xebialabs.jello.watch.DragWatcherActor.Tick

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import com.xebialabs.jello.domain._

class Jello(jira: Jira, trello: Trello) extends LazyLogging { self: ConfigAware =>

  /**
   * Returns successful future if all permissions are in order. Failure otherwise.
   */
  def validateTrelloPermissions(): Future[Unit] = {

    val tokenFuture = trello.getTokenInfo

    tokenFuture.onFailure { case e =>
      logger.error(s"Could not validate trello token. Have you configured one? Or is it expired? Please open this URL in browser ${config.trello.apiUri}/authorize?key=${config.trello.appKey}&name=Jello&expiration=30days&response_type=token&scope=read,write and after you get the token, save it into conf/application.conf.")
      throw e
    }

    tokenFuture.map { tokenInfo =>
      tokenInfo.permissions.find(_.idModel == "*") match {
        case Some(starPerm) =>
          if (!starPerm.read || !starPerm.write) {
            throw new RuntimeException("Jello application should have read and write permissions.")
          }

          logger.info(s"Using token for ${tokenInfo.identifier}, which has been created at ${tokenInfo.dateCreated} and will expire at ${tokenInfo.dateExpires}")

        case None =>
          throw new RuntimeException("Can not find permissions for model '*'")
      }
    }

  }

  def getTicketsForEstimation: Future[Seq[Ticket]] = Try(config.jira.query) match {
    case Success(jql) =>
      jira.search(jql)
    case Failure(e) =>
      logger.error("Only JQL input is supported currently", e)
      die(1)
      null
  }

  def prepareForEstimation(ticketsFuture: Future[Seq[Ticket]], title: String) = for (
    tickets   <- ticketsFuture;
    board     <- trello.createBoard(title);
    cards     <- board.putTickets(tickets)
  ) yield board

  def saveEstimationsFrom(board: Board): Future[Unit] = board.getColumns.flatMap { columns =>

    val tickets: Seq[Ticket] = columns.filter(_.name.isNumber).flatMap { column =>
      column.cards.map {
        card => Ticket(
          card.name.split(" ").head,
          card.name.split(" ").tail.mkString(" "),
          Some(column.name.toInt)
        )
      }
    }

    Future.sequence(tickets.map(jira.updateEstimation))
  } map  {
    _ => trello.closeBoard(board.id)
  }

  def handleEstimation(board: Board): Future[Unit] = {
    val token = system.scheduler.schedule(1 second, 1 second, system.actorOf(DragWatcherActor.props(board)), Tick)

    askForInput(s"Please do the estimations at: ${board.shortUrl} and press enter when you're done.") andThen {
      case _ => token.cancel()
    } map {
      _ => ()
    }
  }

}

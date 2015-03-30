package com.xebialabs.jello

import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.jello.conf.ConfigAware
import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.Trello.Board
import com.xebialabs.jello.domain.{Jira, Trello}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class Jello(jira: Jira, trello: Trello) extends LazyLogging { self: ConfigAware =>

  /**
   * This is a blocking call, which prints information about trello application token to the log, or throws an exception in case of no token of insufficient rights.
   */
  def validateSettings(): Unit = {

    val tokenFuture = trello.getTokenInfo

    tokenFuture.onFailure({ case e =>
      logger.error(s"Could not validate trello token. Have you configured one? Or is it expired? Please open this URL in browser ${config.trello.apiUri}/authorize?key=${config.trello.appKey}&name=Jello&expiration=30days&response_type=token&scope=read,write and after you get the token, save it into conf/application.conf.")
    })

    val tokenInfo = Await.result(tokenFuture, config.jello.futureTimeout)

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

  def prepareForEstimation(tickets: Seq[String], title: String): Future[Board] = {

    Future.sequence(tickets.map(jira.getTicket)).flatMap {
      case tt =>
        val b = trello.createBoard(title)
        b.flatMap({
          b => b.putTickets(tt)
        })
        b
    }
  }

  def saveEstimationsFrom(board: Board): Future[Unit] = {

    board.getColumns.flatMap { columns =>
      val tickets: Seq[Ticket] = columns
      Future.sequence(tickets.map(jira.updateEstimation))
    } map  {
      tickets => trello.archiveBoard(board.id)
    }

  }

}

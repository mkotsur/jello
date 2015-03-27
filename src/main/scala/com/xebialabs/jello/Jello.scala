package com.xebialabs.jello

import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.Trello.Board
import com.xebialabs.jello.domain.{Jira, Trello}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Jello(jira: Jira, trello: Trello) {

  def validateSettings(): Unit = {

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

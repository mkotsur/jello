package com.xebialabs.jello

import com.xebialabs.jello.domain.Trello.Board
import com.xebialabs.jello.domain.{Jira, Trello}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Jello(jira: Jira, trello: Trello) {

  def prepareForEstimation(tickets: Seq[String]): Future[Board] = {

    Future.sequence(tickets.map(jira.getTicket)).flatMap {
      case tt =>
        val b = trello.createBoard()
        b.flatMap(_.putTickets(tt))
        b
    }
  }

  def saveEstimationsFrom(board: Board): Unit = {
    board.getTickets.foreach(jira.putTicket)
    trello.archiveBoard(board.id)
  }

}

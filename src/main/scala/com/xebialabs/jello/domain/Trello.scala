package com.xebialabs.jello.domain

import java.util.UUID

import com.xebialabs.jello.conf.{DefaultConfig, ConfigAware}
import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.Trello.{Board, Column, _}
import com.xebialabs.jello.domain.json.TrelloProtocol
import com.xebialabs.jello.http.RequestExecutor
import spray.http.HttpResponse
import spray.httpx.SprayJsonSupport._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

object Trello {

  case class Column(id: String, name: String, cards: Seq[Card] = Seq())

  case class Label(id: String, color: String)

  case class Card(id: String, name: String)

  case class TokenPermission(idModel: String, modelType: String, read: Boolean, write: Boolean)

  case class TokenInfo(identifier: String, dateCreated: String, dateExpires: String, permissions: Seq[TokenPermission])

  case class Board(id: String, name: String, shortUrl: String, lists: Seq[Column] = Seq()) extends RequestExecutor with TrelloProtocol with DefaultConfig {

    /**
     * Retrieves estimated tickets from the board.
     */
    def getCards: Future[Seq[Card]] = getColumns.map(_.flatMap(_.cards))

    /**
     * Retrieves columns with cards from the board.
     */
    def getColumns: Future[Seq[Column]] = runRequest[Seq[Column]](ColumnsReq(id))

    /**
     * Appends tickets onto the board
     */
    def putTickets(tickets: Seq[Ticket]): Future[Seq[Card]] = {
      Future.successful(
        tickets.map(t => Await.result(
          runRequest[Card](NewCardReq(name = s"${t.id} ${t.title}", idList = lists.head.id)),
          5 seconds
        ))
      )
    }

    /**
     * Updates card with the first available label on the board
     */
    def updateLabel(card: Card): Future[HttpResponse] = runRequest[Seq[Label]](GetLabelsReq(id)).flatMap { labels =>
      runRequest[HttpResponse](UpdateLabelReq(card.id, labels.head.id))
    }

  }

}

class Trello extends RequestExecutor with TrelloProtocol { this: ConfigAware =>

  /**
   * Returns information about currently configured token.
   */
  def getTokenInfo: Future[TokenInfo] = runRequest[TokenInfo](TokenInfoReq)

  /**
   * Creates a new board with lists defined in the configuration file.
   */
  def createBoard(title: String = UUID.randomUUID().toString): Future[Board] = {

    val columns = config.trello.lists

    def createColumn(board: String, column: String): Future[Column] = {
      runRequest[NewColumnResp](
        (board,ColumnReq(column, columns.indexOf(column) * 10 + 10))
      )
    }

    val boardFuture: Future[Board] = runRequest[BoardResp](NewBoardReq(title))

    boardFuture.map {
      case b: Board =>
        val createdColumns = columns.map(c => {
          // Blocking because of bug in Trello :-(
          Await.result(createColumn(b.id, c), 10 second)
        })
        b.copy(lists = createdColumns)
    }
  }

  /**
    * Lists boards of a member (by username or member id).
    */
  def listBoards(member: String): Future[Seq[Board]] = {
    runRequest[Seq[BoardResp]](ListBoardsReq(member))
  }

  /**
    * Deletes a board of a member.
    */
  def deleteBoard(boardId: String, member: String) = {
    runRequest[HttpResponse](DeleteBoardReq(boardId, member))
  }

  /**
   * Archives a board.
   * //TODO: move into Board class
   */
  def closeBoard(id: String) = runRequest[BoardResp](CloseBoardReq(id, value = true))


}

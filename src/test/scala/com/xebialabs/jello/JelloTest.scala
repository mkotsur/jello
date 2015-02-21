package com.xebialabs.jello

import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.Trello.Board
import com.xebialabs.jello.domain.{Jira, Trello}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Future, Promise}

class JelloTest extends TestSugar with MockitoSugar {

  var jello: Jello = _
  var jira: Jira = _
  var trello: Trello = _
  var myBoard: Board = _

  val t1 = Ticket("T-1", "Create feature")
  val t2 = Ticket("T-2", "Fix bug")

  val p1 = Promise[Ticket]()
  val p2 = Promise[Ticket]()
  val boardPromise = Promise[Board]()

  override protected def beforeEach() {
    jira = mock[Jira]

    when(jira.getTicket("T-1")).thenReturn(p1.future)
    when(jira.getTicket("T-2")).thenReturn(p2.future)

    trello = mock[Trello]

    jello = new Jello(jira, trello)

    myBoard = mock[Board]
    when(myBoard.id).thenReturn("my-board-id")

    when(trello.createBoard()).thenReturn(boardPromise.future)
  }

  describe("jello application") {

    it("should get tickets from JIRA and put them onto new board") {
      jello.prepareForEstimation(Seq("T-1", "T-2"))
      p1.success(t1)
      p2.success(t2)
      boardPromise.success(myBoard)
      verify(trello).createBoard()
      verify(myBoard).putTickets(Seq(t1, t2))
    }

    it("save estimated tickets back to JIRA and delete board") {

      val estimatedT1 = t1.copy(estimation = Some(3))
      val estimatedT2 = t2.copy(estimation = Some(2))

      when(trello.getTickets("my-board-id")).thenReturn(Future(Seq(estimatedT1, estimatedT2)))
      when(jira.updateEstimation(estimatedT1)).thenReturn(Future(estimatedT1))
      when(jira.updateEstimation(estimatedT2)).thenReturn(Future(estimatedT2))

      jello.saveEstimationsFrom("my-board-id").futureValue

      verify(jira).updateEstimation(estimatedT1)
      verify(jira).updateEstimation(estimatedT2)
      verify(trello).archiveBoard("my-board-id")
    }
  }
}

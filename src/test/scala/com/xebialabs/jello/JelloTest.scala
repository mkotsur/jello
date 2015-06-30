package com.xebialabs.jello

import com.xebialabs.jello.conf.DefaultConfig
import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.Trello._
import com.xebialabs.jello.domain.{Jira, Trello}
import com.xebialabs.jello.support.UnitTestSugar
import org.mockito.Mockito._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

class JelloTest extends UnitTestSugar {

  var jello: Jello = _
  var jira: Jira = _
  var trello: Trello = _
  var myBoard: Board = _

  val t1 = Ticket("T-1", "Title 1")
  val t2 = Ticket("T-2", "Title 2")

  val p1 = Promise[Ticket]()
  val p2 = Promise[Ticket]()
  val boardPromise = Promise[Board]()

  override protected def beforeEach() {

    jira = mock[Jira]
    trello = mock[Trello]

    when(jira.getTicket("T-1")).thenReturn(p1.future)
    when(jira.getTicket("T-2")).thenReturn(p2.future)

    jello = new Jello(jira, trello) with DefaultConfig

    myBoard = mock[Board]
    when(myBoard.id).thenReturn("my-board-id")

    when(trello.createBoard("My board")).thenReturn(boardPromise.future)
  }

  describe("jello application") {

    it("should get tickets from JIRA and put them onto new board") {
      jello.prepareForEstimation(Future(Seq(Ticket("T-1", "Title 1"), Ticket("T-2", "Title 2"))), "My board")
      p1.success(t1)
      p2.success(t2)
      boardPromise.success(myBoard)
      verify(trello).createBoard("My board")
      verify(myBoard).putTickets(Seq(t1, t2))
    }

    it("save estimated tickets back to JIRA and delete board") {

      val estimatedT1 = t1.copy(estimation = Some(3))
      val estimatedT2 = t2.copy(estimation = Some(2))

      val cHome = Column("cHome", "Home", Seq(Card("c1", "T-3 Title 3")))
      val c3 = Column("c3", "3", Seq(Card("c1", "T-1 Title 1")))
      val c2 = Column("c2", "2", Seq(Card("c2", "T-2 Title 2")))

      val myBoard = mock[Board]
      when(myBoard.id).thenReturn("my-board-id")

      when(myBoard.getColumns).thenReturn(Future(Seq(cHome, c3, c2)))
      when(jira.updateEstimation(estimatedT1)).thenReturn(Future(estimatedT1))
      when(jira.updateEstimation(estimatedT2)).thenReturn(Future(estimatedT2))

      whenReady(jello.saveEstimationsFrom(myBoard)) { _ =>
        verify(jira).updateEstimation(estimatedT1)
        verify(jira).updateEstimation(estimatedT2)
        verify(trello).archiveBoard("my-board-id")
      }

    }

    it("should throw an exception when token grants no permissions") {
      val f = Future(TokenInfo("J", "2015.05.05", "2016.05.05", Seq()))
      when(trello.getTokenInfo).thenReturn(f)

      whenReady(jello.validateTrelloPermissions().failed) { e =>
        e shouldBe a [RuntimeException]
        e.getMessage shouldBe "Can not find permissions for model '*'"
      }

    }

    it("should throw an exception when token grants only read permissions") {
      val tp = TokenPermission("*", "?", read = true, write = false)
      val f = Future(TokenInfo("J", "2015.05.05", "2016.05.05", Seq(tp)))
      when(trello.getTokenInfo).thenReturn(f)

      whenReady(jello.validateTrelloPermissions().failed) { e =>
        e shouldBe a [RuntimeException]
        e.getMessage should include("should have read and write permission")
      }
    }

    it("should validate settings successfully when when token is in order") {
      val tp = TokenPermission("*", "?", read = true, write = true)
      val f = Future(TokenInfo("J", "2015.05.05", "2016.05.05", Seq(tp)))
      when(trello.getTokenInfo).thenReturn(f)

      whenReady(jello.validateTrelloPermissions()) { e =>
        e should be ()
      }
    }

  }
}

package com.xebialabs.jello.domain.tickets

import com.xebialabs.jello.domain.Jira
import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.tickets.RangeConverter.TicketsParser
import com.xebialabs.jello.support.UnitTestSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import shapeless.{HNil, ::}
import shapeless.ops.hlist.Prepend
import spray.http.HttpResponse
import spray.httpx.UnsuccessfulResponseException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class RangeConverterTest extends UnitTestSugar {

  implicit var jira: Jira = _

  val rc = new RangeConverter {}

  val t1 = Ticket("T-1", "Create a feature")
  val t2 = Ticket("T-2", "Fix a bug")
  val t3 = Ticket("T-3", "Solve a problem")

  val r4 = Ticket("R-4", "R4")
  val r5 = Ticket("R-5", "R5")
  val r6 = Ticket("R-6", "R6")

  override protected def beforeAll(): Unit = {
    jira = mock[Jira]

    when(jira.getTicket(anyString())).thenReturn(Future.failed(new UnsuccessfulResponseException(HttpResponse(404))))
    when(jira.getTicket("T-1")).thenReturn(Future(t1))
    when(jira.getTicket("T-2")).thenReturn(Future(t2))
    when(jira.getTicket("T-3")).thenReturn(Future(t3))


    when(jira.getRapidBoardTickets(3, "R-4", "R-4")).thenReturn(Future(Seq(r4)))
    when(jira.getRapidBoardTickets(3, "R-4", "R-5")).thenReturn(Future(Seq(r4, r5)))
    when(jira.getRapidBoardTickets(3, "R-4", "R-6")).thenReturn(Future(Seq(r4, r5, r6)))
    when(jira.getRapidBoardTickets(3, "R-5", "R-6")).thenReturn(Future(Seq(r5, r6)))

  }

  describe("range converter") {


    it("should resolve comma separated sequences") {
      rc.inputToTickets("T-1, T-2, T-3").futureValue shouldEqual Seq(t1, t2, t3)
      rc.inputToTickets("T-1,  T-2,T-3").futureValue shouldEqual Seq(t1, t2, t3)
    }

    it("should throw an exception for not found ticket") {
      whenReady(rc.inputToTickets("X-0, T-1,  X-4").failed) { ex =>
        ex.getMessage shouldBe "Ticket X-0 could not be found"
      }
    }

    it("should resolve double-dot-separated ranges") {
      rc.inputToTickets("3@R-4..R-6").futureValue shouldEqual Seq(r4, r5, r6)
    }

    it("should throw and exception when range does not contain board id") {
      whenReady(rc.inputToTickets("R-4..R-5").failed) { ex =>
        ex.getMessage should startWith("Invalid input '.'")
      }
    }

    it("should throw and exception when issue ID does not contain dash") {
      whenReady(rc.inputToTickets("R4").failed) { ex =>
        ex.getMessage should startWith("Invalid input '4'")
      }
    }

    it("should resolve mixed sequence/range inputs") {
      rc.inputToTickets("T-1, T-2, 3@R-4..R-5").futureValue shouldEqual Seq(t1, t2, r4, r5)
    }

    it("should resolve ranges that consist out of a single ticket") {
      rc.inputToTickets("T-1, T-2, 3@R-4..R-4").futureValue shouldEqual Seq(t1, t2, r4)
    }

    it("should resolve multiple ranges") {
      rc.inputToTickets("T-1, T-2, 3@R-4..R-4, 3@R-5..R-6").futureValue shouldEqual Seq(t1, t2, r4, r5, r6)
    }

  }


}

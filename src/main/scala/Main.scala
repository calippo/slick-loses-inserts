import java.util.concurrent.Executors

import slick.lifted.{TableQuery, Tag}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {
  val db = Database.forConfig("db")

  val txn = TableQuery[Txn]

  Await.result(db.run(txn.schema.drop).recover{ case e => println(e.getLocalizedMessage) }, Duration.Inf)
  Await.result(db.run(txn.schema.create), Duration.Inf)

  var result = 1
  var count = 0
  val max = 1000
  val batch = 100

  (1 to max).foreach { i =>
    count += batch
    println(s"iteration $i")

    val insertionResult = Await.result(insertTxnBatch(result, batch), Duration.Inf)
    result = result + insertionResult
  }

  Await.result(db.run(txn.size.result).map { size =>
    println(s"expected size: $count\nactual size: $size")
  }, Duration.Inf)

  def seeds(first: Int, batch: Int) = (first+1 to first+batch).map {
    x => (x, "META")
  }.toList

  def insertTxnBatch(first: Int, batch: Int): Future[Int] = {
    val txns = seeds(first, batch)

    val last = txns.lastOption.map(_._1).getOrElse(1)
    println("insert batch")

    val action = txn ++= txns
    val future = db.run(action)

    future.map { _ => last }
  }
}

class Txn(tag: Tag) extends Table[(Int, String)](tag, "TXN") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
  def metadata = column[String]("METADATA")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (id, metadata)
}

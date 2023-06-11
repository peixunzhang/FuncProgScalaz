package dda

import scala.concurrent.duration._
import scalaz.NonEmptyList
import scalaz.Monad
import scalaz.syntax.std.list._
import scalaz.syntax.all._
// import scalaz.syntax.applicative._
import scalaz.Applicative
import scalaz.Equal
import scalaz.Show
import scalaz.Cord
import scalaz.Order
import scalaz.std.string._
// import scalaz.syntax.monad._
  
final case class Epoch(millis: Long) extends AnyVal {
  def +(d:FiniteDuration): Epoch = Epoch(millis+d.toMillis)
  def -(e: Epoch): FiniteDuration = (millis - e.millis).millis
}

trait Drone[F[_]] {
  def getBacklog: F[Int]
  def getAgents: F[Int]
}

final case class MachineNode(id: String)

object MachineNode {
  implicit val equal: Equal[MachineNode] = new Equal[MachineNode] {
    def equal(a1: MachineNode, a2: MachineNode): Boolean = a1 == a2
  }
  implicit val order: Order[MachineNode] =Order.apply[String].contramap(_.id)

  implicit val show: Show[MachineNode] = new Show[MachineNode] {
    def show(f: MachineNode): Cord = Cord(f.id)
  }
}

trait Machines[F[_]] {
  def getTime: F[Epoch]
  def getManaged: F[NonEmptyList[MachineNode]]
  def getAlive: F[Map[MachineNode, Epoch]]
  def start(node: MachineNode): F[MachineNode]
  def stop(node: MachineNode): F[MachineNode]
}

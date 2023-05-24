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
trait Machines[F[_]] {
  def getTime: F[Epoch]
  def getManaged: F[NonEmptyList[MachineNode]]
  def getAlive: F[Map[MachineNode, Epoch]]
  def start(node: MachineNode): F[MachineNode]
  def stop(node: MachineNode): F[MachineNode]
}

final case class WorldView(
  backlog: Int,
  agents: Int,
  managed: NonEmptyList[MachineNode],
  alive: Map[MachineNode, Epoch],
  pending: Map[MachineNode, Epoch],
  time: Epoch
)

object WorldView {
  implicit val equal: Equal[WorldView] = new Equal[WorldView] {
    def equal(a1: WorldView, a2: WorldView): Boolean = a1 == a2
  }

  implicit val show: Show[WorldView] = new Show[WorldView] {
    def show(f: WorldView): Cord = Cord(f.toString())
  }
}

trait DynAgents[F[_]] {
  def initial: F[WorldView]
  def update(old: WorldView): F[WorldView]
  def act(world: WorldView): F[WorldView]
}

final class DynAgentsModule[F[_]: Applicative](D: Drone[F], M: Machines[F]) extends DynAgents[F] {

  def initial: F[WorldView] = {
    (D.getBacklog |@| D.getAgents |@| M.getManaged |@| M.getAlive |@| M.getTime)(WorldView(_, _, _, _, Map.empty, _))
  }
    
  def update(old: WorldView): F[WorldView] = { 
    for {
      snap <- initial
      changed = symdiff(old.alive.keySet, snap.alive.keySet)
      pending = (old.pending -- changed).filterNot {
        case (_, started) => (snap.time - started) >= 10.minutes
      }
      update = snap.copy(pending = pending)
    } yield update
}

  private def symdiff[T](a: Set[T], b: Set[T]): Set[T] = (a union b) -- (a intersect b)
  
  def act(world: WorldView): F[WorldView] = world match {
    case NeedsAgent(node) => 
      M.start(node) >| world.copy(pending = Map(node -> world.time))

    case Stale(nodes) =>
      nodes.traverse { node =>
        M.stop(node) >| node
      }.map { stopped =>
        val updates = stopped.strengthR(world.time).toList.toMap
        world.copy(pending = world.pending ++ updates)
      }
    
    case _ => world.pure[F]
  }
  
  private object NeedsAgent {
    def unapply(world: WorldView): Option[MachineNode] = world match {
      case WorldView(backlog, 0, managed, alive, pending, _)
        if backlog > 0 && alive.isEmpty && pending.isEmpty => Option(managed.head)
      case _ => None
    }
  }

  private object Stale {
    def unapply(world: WorldView): Option[NonEmptyList[MachineNode]] = world match {
      case WorldView(backlog, _, _, alive, pending, time) if alive.nonEmpty =>
        (alive -- pending.keys).collect {
          case (n, started) if  backlog == 0 && (time - started).toMinutes % 60 >= 58 => n
          case (n, started) if (time - started) >= 5.hours => n
        }.toList.toNel.toOption
      case _ => None
    }
  }

}

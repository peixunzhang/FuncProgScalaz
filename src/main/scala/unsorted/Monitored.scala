package unsorted
import scalaz.Functor
import scalaz.Const
import scalaz.NonEmptyList
import scalaz.std.set._
import scalaz.syntax.all._
import dda.WorldView
import dda.MachineNode
import dda.DynAgentsModule
import dda.Epoch
import dda.Machines
import dda.Drone
import dda.DynAgents

final class Monitored[U[_]: Functor](program: DynAgents[U]) {
  type F[a] = Const[Set[MachineNode], a]

  private val D = new Drone[F] {
    def getAgents: F[Int] = Const(Set.empty)

    def getBacklog: F[Int] = Const(Set.empty)
  }

  private val M = new Machines[F] {
    def getTime: F[Epoch] = Const(Set.empty)
    
    def getManaged: F[NonEmptyList[MachineNode]] = Const(Set.empty)
    
    def getAlive: F[Map[MachineNode,Epoch]] = Const(Set.empty)
    
    def start(node: MachineNode): F[MachineNode] = Const(Set.empty)
    
    def stop(node: MachineNode): F[MachineNode] = Const(Set(node))
  }

  val monitor = new DynAgentsModule[F](D, M)

  def act(world: WorldView): U[(WorldView, Set[MachineNode])] = {
    val stopped = monitor.act(world).getConst
    program.act(world).strengthR(stopped)
  }
}

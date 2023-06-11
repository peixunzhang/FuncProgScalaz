package dda

import Data._
import scala.collection.immutable.IntMap
import java.time.Instant
import scalaz.NonEmptyList
import scalaz.Functor
import scalaz.Const
import scalaz.std.string._
import scalaz.Id.Id
import time._
import scalaz.syntax.all._
import scalaz.std.set._
import scalaz.std.tuple._
import scalaz.Equal
import scalaz.State.{get, modify}
import scalaz.State
import time._
import unsorted.Monitored

object Data {
  val node1 = MachineNode("1243d1af-828f-4ba3-9fc0-a19d86852b5a")
  val node2: MachineNode                 = MachineNode("550c4943-229e-47b0-b6be-3d686c5f013f")
  val managed: NonEmptyList[MachineNode] = NonEmptyList(node1, node2)

  val time1: Epoch = epoch"2017-03-03T18:07:00Z"
  val time2: Epoch = epoch"2017-03-03T18:59:00Z" // +52 mins
  val time3: Epoch = epoch"2017-03-03T19:06:00Z" // +59 mins
  val time4: Epoch = epoch"2017-03-03T23:07:00Z" // +5 hours

  val needsAgents: WorldView =
    WorldView(5, 0, managed, Map.empty, Map.empty, time1)
}

object ConstImpl {
  type F[a] = Const[String, a]

  private val D = new Drone[F] {
    def getBacklog: F[Int] = Const("backlog")

    def getAgents: F[Int] = Const("agents")
  }

  private val M = new Machines[F] {
    def getTime: F[Epoch] = Const("time")
    
    def getManaged: F[NonEmptyList[MachineNode]] = Const("manage")
    
    def getAlive: F[Map[MachineNode,Epoch]] = Const("alive")
    
    def start(node: MachineNode): F[MachineNode] = Const("start")
    
    def stop(node: MachineNode): F[MachineNode] = Const("stop")
  }
  val program = new DynAgentsModule[F](D, M)
}

class Mutable(state: WorldView) {
  var started, stopped: Int = 0

  private val D: Drone[Id] = new Drone[Id] {
    def getBacklog: Int = state.backlog

    def getAgents: Int = state.agents
  }

  private val M: Machines[Id] = new Machines[Id] {
    def getTime: Epoch = state.time
    
    def getManaged: NonEmptyList[MachineNode] = state.managed
    
    def getAlive: Map[MachineNode,Epoch] = state.alive
    
    def start(node: MachineNode): MachineNode = {started += 1 ; node}
    
    def stop(node: MachineNode): MachineNode = {stopped += 1 ; node}
    
  }

  val program = new DynAgentsModule[Id] (D, M)
}

final case class World (
  backlog: Int,
  agents: Int,
  managed: NonEmptyList[MachineNode],
  alive: Map[MachineNode, Epoch],
  started: Set[MachineNode],
  stopped: Set[MachineNode],
  time: Epoch
)

object StateImpl {
  type F[a] = State[World, a]

  private val D = new Drone[F] {
    def getBacklog: F[Int] = get.map(_.backlog)
    def getAgents: F[Int] = get.map(_.agents)    
  }

  private val M = new Machines[F] {
    def getTime: F[Epoch] = get.map(_.time)
    
    def getManaged: F[NonEmptyList[MachineNode]] = get.map(_.managed)
    
    def getAlive: F[Map[MachineNode,Epoch]] = get.map(_.alive)
    
    def start(node: MachineNode): F[MachineNode] = modify[World](w => w.copy(started = w.started + node)).as(node)
    
    def stop(node: MachineNode): F[MachineNode] = modify[World](w => w.copy(stopped = w.stopped + node)).as(node)
  }
  val program = new DynAgentsModule[F](D, M)
}



final class DynAgentsModuleSpec extends Test {
  "Business Logic" should "generate an initial world view" in {
    val mutable = new Mutable(needsAgents)
    import mutable._

    program.initial shouldBe needsAgents
  }
  it should "call the expected methods" in {
    import ConstImpl._

    val alive = Map(node1 -> time1, node2 -> time2)
    val world = WorldView(1, 1, managed, alive, Map.empty, time4)

    program.act(world).getConst shouldBe "stopstop"
  }

  it should "monitor stopped nodes" in {
    val underlying = new Mutable(needsAgents).program

    val alive = Map(node1 -> time1, node2 -> time1)
    val world = WorldView(1, 1, managed, alive, Map.empty, time4)
    val expected = world.copy(pending = Map(node1 -> time4, node2 -> time4))

    val monitored = new Monitored(underlying)
    monitored.act(world) shouldBe (expected -> Set(node1, node2))
  }

  it should "request agents when needed" in {
    val world1 = World(5, 0, managed, Map.empty, Set.empty, Set.empty, time1)
    val view1 = WorldView(5, 0, managed, Map.empty, Map.empty, time1)

    val (world2, view2) = StateImpl.program.act(view1).run(world1)

    view2.shouldBe(view1.copy(pending = Map(node1 -> time1)))
    world2.stopped.shouldBe(world1.stopped)
    world2.started.shouldBe(Set(node1))
  }
}

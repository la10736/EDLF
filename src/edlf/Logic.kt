package edlf

import graph.*
import kotlin.reflect.jvm.internal.impl.javax.inject.Inject

/**
 * Logic is our logic schema. Provide methods to create logic components and
 * inputs nodes where to post new changes. Every components can define one or more
 * outputs points.
 * All inputs and outputs point have
 *
 * - unique name
 * - finite state value
 * - state description
 *
 * Is it possible to register a queue to get all output change notifications.
 *
 * Every logic surround a DAG graph and cannot contains cycles. Every nodes can be configured
 * after build it. The logic cannot change after first event (it is not dynamic).
 */


open class Components<T>() {
    var outEvents = ChangeEvents<T>()
    internal var g = DAG<Component<T>>()

    fun process(changedNode: Input<T>) {
        ProcessAlgorithm(changedNode).process()
    }

    open fun input(initialValue: T) = Input(this, initialValue)
    open fun output(input: Component<T>) = Output(this, input)
}

class Logic() : Components<Boolean>() {

    fun input() = super.input(false)

    fun not(input: Component<Boolean>) = Not(this, input)

    fun xor(input0: Component<Boolean>, input1: Component<Boolean>) = Xor(this, input0, input1)

    fun nand(input0: Component<Boolean>, input1: Component<Boolean>) = Nand(this, input0, input1)

}

class ProcessAlgorithm<T>(changedNode: Input<T>){
    private var toProcess = mutableListOf<Component<T>>(changedNode)
    var sourcesMap = sourcesMap(changedNode)

    fun process() {
        while (toProcess.isNotEmpty()) {
            var node = toProcess.removeAt(0)
            toProcess.addAll(node.processChanged(this))
        }
    }

    fun processWire(source: Component<T>, destination: Component<T>) {
        sourcesMap[destination]!!.remove(source)
    }

    fun processableNode(node: Component<T>): Boolean {
        return sourcesMap[node]!!.isEmpty()
    }

    fun processAllWire(node: Component<T>) {
        node.allWires().map {
            pair ->
            var (source, dest) = pair
            processWire(source, dest)
        }
    }

    private fun sourcesMap(node: Component<T>) : MutableMap<Component<T>, MutableSet<Component<T>>> {
        var sourcesMap = mutableMapOf<Component<T>, MutableSet<Component<T>>>()
        node.allWires().map {
            wire ->
            var (source, dest) = wire
            if (dest !in sourcesMap) {
                sourcesMap.put(dest, mutableSetOf<Component<T>>())
            }
            sourcesMap[dest]!!.add(source)
        }
        return sourcesMap
    }

}

abstract class Component<T>(var owner: Components<T>, inputs: List<Component<T>>) {
    protected val inputs = inputs
    private var inputStates = getInputsStates()
    protected val node = owner.g.addNode(this)

    init {
        inputs.forEach { owner.g.addEdge(it, this) }
    }

    constructor(owner: Components<T>, vararg inputs: Component<T>) : this(owner, inputs.asList())

    protected var state = logic()

    abstract fun logic(): T

    fun value(): T {
        return state
    }

    fun dependedComponents(): Collection<Component<T>>{
        return node.outputs()
    }

    fun allWires(): Collection<Pair<Component<T>,Component<T>>>{
        return node.bfsEdges()
    }

    open internal fun processChanged(algorithm: ProcessAlgorithm<T>):
            Collection<Component<T>>{
        var changedComponents = mutableListOf<Component<T>>()
        for (child in dependedComponents()){
            algorithm.processWire(this, child)
            if (algorithm.processableNode(child)) {
                if (child.update()){
                    changedComponents.add(child)
                }else{
                    algorithm.processAllWire(child)
                }
            }
        }
        return changedComponents
    }

    private fun getInputsStates() = inputs.map { it.value() }

    protected fun compute() {
        if (!stateChanged()) {
            throw RuntimeException("Call compute without change source")
        }
        stateUpdate()
        state = logic()
    }

    internal fun update(): Boolean {
        var old = state
        compute()
        return old != state
    }

    private fun stateChanged(): Boolean {
        if (inputStates.size == 0) {
            return true
        }
        var newStates = getInputsStates()
        for (i in 0..(inputStates.size - 1)) {
            if (newStates[i] != inputStates[i]) {
                return true
            }
        }
        return false
    }

    private fun stateUpdate() {
        inputStates = getInputsStates()
    }
}

class Input<T>(owner: Components<T>, initialState: T) : Component<T>(owner) {
    init {
        state = initialState
    }

    override fun logic(): T {
        return newState
    }

    private var newState = initialState

    fun set(value: T) {
        newState = value
        if (update()) {
            owner.process(this)
        }
    }
}


class Output<T>(owner: Components<T>, source: Component<T>) : Component<T>(owner, source) {
    val source: Component<T>
        get() = inputs[0]

    override fun logic(): T {
        return source.value()
    }

    override fun processChanged(algorithm: ProcessAlgorithm<T>):
            Collection<Component<T>>{
        owner.outEvents.push(this)
        return emptyList()
    }
}

class Not(owner: Logic, source: Component<Boolean>) : Component<Boolean>(owner, source) {
    val source: Component<Boolean>
        get() = inputs[0]

    override fun logic(): Boolean {
        return !source.value()
    }

}

class Xor(owner: Logic, inputs0: Component<Boolean>, input1: Component<Boolean>) :
        Component<Boolean>(owner, inputs0, input1) {
    val pin0: Component<Boolean>
        get() = inputs[0]
    val pin1: Component<Boolean>
        get() = inputs[1]

    override fun logic(): Boolean {
        return pin0.value() != pin1.value()
    }

}

class Nand(owner: Logic, inputs0: Component<Boolean>, input1: Component<Boolean>) :
        Component<Boolean>(owner, inputs0, input1) {
    val pin0: Component<Boolean>
        get() = inputs[0]
    val pin1: Component<Boolean>
        get() = inputs[1]

    override fun logic(): Boolean {
        return !(pin0.value() && pin1.value())
    }

}

class ChangeEvents<T>() {
    private val queue = mutableListOf<Event<T>>()

    class Event<T>(var node: Output<T>, var value: T)

    fun pop(): ChangeEvents.Event<T> {
        var ret = queue.first()
        queue.removeAt(0)
        return ret
    }

    fun isEmpty(): Boolean {
        return this.queue.isEmpty()
    }

    internal fun push(node: Output<T>) {
        queue.add(Event<T>(node, node.value()))
    }

}
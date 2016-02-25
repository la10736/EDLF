/**
 * Test case for simple logic ports
 *
 * Created by michele on 15/02/16.
 */

package edlf

import kotlin.test.*
import org.junit.*

class TestLogic {
    val logic = Logic()
    val input = logic.input()
    val output = logic.output(input)
    val queue = logic.outEvents

    fun assertEvent(event: ChangeEvents.Event<Boolean>, output: Output<Boolean>,
                    value: Boolean) {
        assertEquals(output, event.node)
        assertEquals(value, event.value)
    }

    @Test
    fun base() {
        with(queue) {
            assert(isEmpty())
            input.set(true)
            assertFalse { isEmpty() }
            var event = pop()
            assertTrue { isEmpty() }
            assertEvent(event, output, true)
        }
    }

    @Test
    fun onOff() {
        with(queue) {
            input.set(true)
            input.set(false)
            assertEvent(queue.pop(), output, true)
            assertEvent(queue.pop(), output, false)
            assertEmpty()
        }
    }

    @Test
    fun noChange() {
        with(queue) {
            input.set(false)
            assertEmpty()
        }
    }

    @Test
    fun not() {
        val input = logic.input()
        val not = logic.not(input)
        val output = logic.output(not)
        with(queue) {
            input.set(true)
            input.set(false)
            assertEvent(queue.pop(), output, false)
            assertEvent(queue.pop(), output, true)
            assertEmpty()
        }
    }

    @Test
    fun xor() {
        val input0 = logic.input()
        val input1 = logic.input()
        val xor = logic.xor(input0, input1)
        val output = logic.output(xor)
        with(queue) {
            input0.set(true)
            input1.set(true)
            input0.set(false)
            input1.set(false)
            assertEvent(queue.pop(), output, true)
            assertEvent(queue.pop(), output, false)
            assertEvent(queue.pop(), output, true)
            assertEvent(queue.pop(), output, false)
            assertEmpty()
        }
    }

    @Test
    fun nand() {
        val input0 = logic.input()
        val input1 = logic.input()
        val nand = logic.nand(input0, input1)
        val output = logic.output(nand)
        with(queue) {
            input0.set(true)
            assertEmpty()
            input1.set(true)
            assertEvent(queue.pop(), output, false)
            input0.set(false)
            assertEvent(queue.pop(), output, true)
            input1.set(false)
            assertEmpty()
        }
    }

    @Test
    fun atomic() {
        val input = logic.input()
        val nand = logic.nand(input, input)
        val nand2 = logic.nand(input, input)
        val xor = logic.xor(nand, nand2)
        logic.output(xor)
        with(queue) {
            input.set(true)
            input.set(false)
            input.set(true)
            assertEmpty()
        }
    }

    fun assertEmpty() {
        assert(queue.isEmpty())
    }

    @Test
    fun notAsNand() {
        val input = logic.input()
        val nand = logic.nand(input, input)
        val output = logic.output(nand)
        with(queue) {
            input.set(true)
            assertEvent(pop(), output, false)
            input.set(false)
            assertEvent(pop(), output, true)
            assertEmpty()
        }
    }

    @Test
    fun callComponentComputeJustOnce() {
        class FakeComponent(owner: Logic, input: Component<Boolean>) :
                Component<Boolean>(owner, input) {
            val input: Component<Boolean>
                get() = inputs[0]

            override fun logic(): Boolean {
                return input.value()
            }

            fun callCompute() {
                compute()
            }

            fun changeInputState(v: Boolean) {
                input.state = v
            }
        }

        val fc = FakeComponent(logic, input)
        input.set(false)
        assertFails {
            fc.callCompute()
        }
        fc.changeInputState(true)
        fc.callCompute()
    }
}

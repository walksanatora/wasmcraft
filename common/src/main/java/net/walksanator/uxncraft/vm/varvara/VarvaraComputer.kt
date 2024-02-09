package net.walksanator.uxncraft.vm.varvara

import net.walksanator.uxncraft.blocks.TerminalEntity
import net.walksanator.uxncraft.vm.Computer
import net.walksanator.uxncraft.vm.Uxn
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayDeque

class VarvaraComputer(val termina: TerminalEntity, var fuel: Int) : Computer() {

    @OptIn(ExperimentalUnsignedTypes::class)
    val cpu = Uxn(ByteArray(0x1000))

    val system = SystemDevice(cpu)
    val console = ConsoleDevice()

    val eventQueue: ArrayDeque<Pair<Short, Consumer<Computer>>> = ArrayDeque()

    init {
        cpu.devices[0] = Optional.of(system)
        cpu.devices[1] = Optional.of(console)
    }

    override fun run() {
        while (fuel > 0) {
            val res = cpu.step()
            if (res.right().isPresent) {
                break // we throad up
            }
            if (res.left().isPresent) {
                if (res.left().get()) {
                    break //we have broken out
                }
            }
        }
    }

    override fun queue(startpos: Short, prerun: Consumer<Computer>) {
        eventQueue.addFirst(Pair(startpos,prerun))
    }
}
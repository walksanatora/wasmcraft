package net.walksanator.uxncraft.vm.varvara

import net.walksanator.uxncraft.blocks.TerminalEntity
import net.walksanator.uxncraft.vm.Uxn
import java.util.*

class VarvaraComputer(val termina: TerminalEntity) {
    @OptIn(ExperimentalUnsignedTypes::class)
    val cpu = Uxn(UByteArray(0x1000))
    val system = SystemDevice(cpu)

    init {
        cpu.devices[0] = Optional.of(system)
    }
}
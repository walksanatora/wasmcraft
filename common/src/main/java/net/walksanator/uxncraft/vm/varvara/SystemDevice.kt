package net.walksanator.uxncraft.vm.varvara

import net.walksanator.uxncraft.vm.Device
import net.walksanator.uxncraft.vm.Uxn
import net.walksanator.uxncraft.vm.msbToUShort
import net.walksanator.uxncraft.vm.toUBytes

class SystemDevice(uxn: Uxn) : Device(uxn) {
    var lastExpansion: UShort = 0x0000u
    var metadataLocation: UShort = 0x0000u// probally later could parse this location for enabling bonus VM features perhaps

    var sysRed: UShort = 0x000Fu
    var sysBlue: UShort = 0xFF77u
    var sysGreen: UShort = 0x7777u

    var state: UByte = 0x00u //literally exitcode

    override fun readByte(address: UByte): UByte {
        return when (address.toInt()) {
            0x00, 0x01, 0x0E -> {0x00u} // RESERVED (no clue)
            0x02 -> readShort(address).toUBytes().first                 //EXPANSION upper
            0x03 -> readShort((address-1u).toUByte()).toUBytes().second //EXPANSION lower
            0x04 -> uxn.ws.sp // working stack stack pointer
            0x05 -> uxn.rs.sp // return stack stack pointer
            0x06 -> readShort(address).toUBytes().first                 //METADATA upper
            0x07 -> readShort((address-1u).toUByte()).toUBytes().second //METADATA lower
            0x08 -> readShort(address).toUBytes().first                 //Red upper
            0x09 -> readShort((address-1u).toUByte()).toUBytes().second //Red lower
            0x0A -> readShort(address).toUBytes().first                 //Green upper
            0x0B -> readShort((address-1u).toUByte()).toUBytes().second //Green lower
            0x0C -> readShort(address).toUBytes().first                 //Blue upper
            0x0D -> readShort((address-1u).toUByte()).toUBytes().second //Blue lower
            0x0F -> state
            else -> {throw IllegalStateException("Unreachable Branch in SystemDevice#readByte")}
        }
    }

    override fun writeByte(address: UByte, byte: UByte) {
        when (address.toInt()) {
            0x00, 0x01 -> {/*nop*/}
            0x02 -> lastExpansion = lastExpansion.and(0x00FFu).or(byte.toUShort())
            0x03 -> lastExpansion = lastExpansion.and(0xFF00u).or(byte.toUShort().rotateLeft(8))
            0x04 -> uxn.ws.sp = byte
            0x05 -> uxn.rs.sp = byte
            0x06 -> metadataLocation = metadataLocation.and(0x00FFu).or(byte.toUShort())
            0x07 -> metadataLocation = metadataLocation.and(0xFF00u).or(byte.toUShort().rotateLeft(8))
            0x08 -> sysRed = sysRed.and(0x00FFu).or(byte.toUShort())
            0x09 -> sysRed = sysRed.and(0xFF00u).or(byte.toUShort().rotateLeft(8))
            0x0A -> sysGreen = sysGreen.and(0x00FFu).or(byte.toUShort())
            0x0B -> sysGreen = sysGreen.and(0xFF00u).or(byte.toUShort().rotateLeft(8))
            0x0C -> sysBlue = sysBlue.and(0x00FFu).or(byte.toUShort())
            0x0D -> sysBlue = sysBlue.and(0xFF00u).or(byte.toUShort().rotateLeft(8))
            0x0E -> {/*TODO: print debugging info*/}
            0x0F -> state = byte
        }
    }

    override fun readShort(address: UByte): UShort {
        return when(address.toInt()) {
            0x00 -> 0x0000u
            0x01 -> readByte(address).msbToUShort(readByte(0x02u))
            0x02 -> lastExpansion
            0x03 -> readByte(address).msbToUShort(readByte(0x04u))
            0x04 -> readByte(address).msbToUShort(readByte(0x05u))
            0x05 -> readByte(address).msbToUShort(readByte(0x06u))
            0x06 -> metadataLocation
            0x07 -> readByte(address).msbToUShort(readByte(0x04u))
            0x08 -> sysRed
            0x09 -> readByte(address).msbToUShort(readByte(0x0Au))
            0x0A -> sysGreen
            0x0B -> readByte(address).msbToUShort(readByte(0x0Cu))
            0x0C -> sysBlue
            0x0D -> readByte(address).msbToUShort(readByte(0x0Eu))
            0x0E -> readByte(address).msbToUShort(readByte(0x0Fu))
            0x0F -> readByte(address).msbToUShort(0x0000u)
            else -> {throw IllegalStateException("Unreachable arm in SystemDevice#readShort")}
        }
    }

    override fun writeShort(address: UByte, short: UShort) {
        when(address.toInt()) {
            0x00 -> {/*nop*/}
            0x01 -> writeByte(0x02u, short.toUBytes().second)
            0x02 -> lastExpansion = short
            0x03 -> { val part = short.toUBytes(); writeByte(address,part.first);writeByte(0x04u,part.second) }
            0x04 -> { val part = short.toUBytes(); writeByte(address,part.first);writeByte(0x05u,part.second) }
            0x05 -> { val part = short.toUBytes(); writeByte(address,part.first);writeByte(0x06u,part.second) }
            0x06 -> metadataLocation = short
            0x07 -> { val part = short.toUBytes(); writeByte(address,part.first);writeByte(0x08u,part.second) }
            0x08 -> sysRed = short
            0x09 -> { val part = short.toUBytes(); writeByte(address,part.first);writeByte(0x0Au,part.second) }
            0x0A -> sysGreen = short
            0x0B -> { val part = short.toUBytes(); writeByte(address,part.first);writeByte(0x0Cu,part.second) }
            0x0C -> sysBlue = short
            0x0D -> { val part = short.toUBytes(); writeByte(address,part.first);writeByte(0x0Eu,part.second) }
            0x0E -> { val part = short.toUBytes(); writeByte(address,part.first);writeByte(0x0Fu,part.second) }
            0x0F -> writeByte(address,short.toUBytes().first)
            else -> {throw IllegalStateException("Unreachable arm in SystemDevice#readShort")}
        }
    }
}
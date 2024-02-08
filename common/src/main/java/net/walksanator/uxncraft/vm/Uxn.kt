package net.walksanator.uxncraft.vm

import com.mojang.datafixers.util.Either
import java.util.*

enum class UxnError {
    Underflow, Overflow, ZeroDiv
}

@OptIn(ExperimentalUnsignedTypes::class)
class Stack {
    val s = UByteArray(0x100) //stack
    var sp: UByte = 0u //stack pointer

    fun updateStackPointer(operandBytes: UByte, resultBytes: UByte, keepMode: Boolean): Optional<UxnError> {
        if (operandBytes > sp) {
            return Optional.of(UxnError.Underflow)
        }

        val newSp: UInt = (if (keepMode) {
            sp
        } else {
            // The subtraction of operandBytes does not need to be checked, as we have already
            // checked that operandBytes <= sp.
            (sp - operandBytes).toUByte()
        } + resultBytes)
        if (newSp > UByte.MAX_VALUE) return Optional.of(UxnError.Overflow)
        sp = newSp.toUByte()

        return Optional.empty()
    }

    fun getByte(offset: UByte): UByte = s[offset.toInt()]
    fun getShort(offset: UByte): UShort {
        val msb = getByte((offset + 1u).toUByte())
        val lsb = getByte(offset)
        return (msb.toUShort().rotateRight(8).or(lsb.toUShort()))
    }

    fun setByte(offset: UByte, byte: UByte) {
        s[(sp - offset).toInt()] = byte
    }

    fun setShort(offset: UByte, short: UShort) {
        val msb: UByte = short.rotateLeft(8).toUByte()
        val lsb: UByte = short.and(0xffu).toUByte()
        setByte((offset + 1u).toUByte(), msb)
        setByte(offset, lsb)
    }
}

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)
class Uxn(val ram: UByteArray) {
    var pc: UShort = 0x100u
    val ws = Stack() // Working Stack
    val rs = Stack() // Run Stack
    val devices = Array<Optional<Device>>(16) { Optional.empty() }

    /**
     * steps VM execution by one instruction
     *
     * @Return left means whether execution is still needed. right means the code crashed
     */
    fun step(): Either<Boolean, UxnError> {
        val instruction = ram[pc.toInt()]
        pc = (pc + 1u).toUShort()

        val keepMode: Boolean = (instruction and 128u).toInt() != 0
        val returnMode: Boolean = (instruction and 64u).toInt() != 0
        val immediate: Boolean = (instruction and 31u).toInt() == 0

        val stack = if (returnMode) {
            rs
        } else {
            ws
        }

        val maskedInstruction = if (immediate) {
            instruction
        } else {
            instruction.and(0b00111111u)
        }

        val t = stack.getByte(1u)
        val n = stack.getByte(2u)
        val l = stack.getByte(3u)
        val h2 = stack.getShort(2u)
        val t2 = stack.getShort(1u)
        val n2 = stack.getShort(3u)
        val l2 = stack.getShort(5u)

        when (maskedInstruction.toInt()) {
            // BRK
            0x00 -> {
                return Either.left(true)
            }
            // JCI
            0x20 -> {
                val msb = ram[pc.toInt()]
                val lsb = ram[(pc + 1u).toInt()]
                pc = (pc + 2u).toUShort()
                val stackret = stack.updateStackPointer(1u, 0u, false)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                if (t.toInt() != 0) {
                    pc = (pc + msb.msbToUShort(lsb)).toUShort()
                }
            }
            // JMI
            0x40 -> {
                val msb = ram[pc.toInt()]
                val lsb = ram[(pc + 1u).toInt()]
                pc = (pc + 2u).toUShort()
                pc = (pc + msb.msbToUShort(lsb)).toUShort()
            }
            // JSI
            0x60 -> {
                val msb = ram[pc.toInt()]
                val lsb = ram[(pc + 1u).toInt()]
                pc = (pc + 2u).toUShort()
                val stackret = rs.updateStackPointer(0u, 2u, false)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                rs.setShort(1u, pc)
                pc = (pc + msb.msbToUShort(lsb)).toUShort()
            }
            // LIT
            0x80 -> {
                val stackret = stack.updateStackPointer(0u, 1u, true)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, ram[pc.toInt()])
                pc = (pc + 1u).toUShort()
            }
            // LIT2
            0xa0 -> {
                val stackret = stack.updateStackPointer(0u, 2u, true)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val msb = ram[pc.toInt()]
                val lsb = ram[(pc + 1u).toInt()]
                stack.setShort(1u, msb.msbToUShort(lsb))
                pc = (pc + 2u).toUShort()
            }
            // LITr
            0xc0 -> {
                val stackret = stack.updateStackPointer(0u, 1u, true)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, ram[pc.toInt()])
                pc = (pc + 1u).toUShort()
            }
            // LIT2r
            0xe0 -> {
                val stackret = stack.updateStackPointer(0u, 2u, true)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val msb = ram[pc.toInt()]
                val lsb = ram[(pc + 1u).toInt()]
                stack.setShort(1u, msb.msbToUShort(lsb))
                pc = (pc + 2u).toUShort()
            }
            // END of immediate instrs
            // INC(2)
            0x01 -> {
                val stackret = stack.updateStackPointer(1u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, (t + 1u).toUByte())
            }

            0x21 -> {
                val stackret = stack.updateStackPointer(2u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, (t2 + 1u).toUShort() )
            }
            // POP(2)
            0x02 -> {
                val stackret = stack.updateStackPointer(1u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
            }
            
            0x22 -> {
                val stackret = stack.updateStackPointer(2u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
            }
            // NIP(2)
            0x03 -> {
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, t)
            }

            0x23 -> {
                val stackret = stack.updateStackPointer(4u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, t2)
            }
            // SWP(2)
            0x04 -> {
                val stackret = stack.updateStackPointer(2u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, n)
                stack.setByte(2u, t)
            }

            0x24 -> {
                val stackret = stack.updateStackPointer(4u, 4u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, n2)
                stack.setShort(3u, t2)
            }
            // ROT(2)
            0x05 -> {
                val stackret = stack.updateStackPointer(3u, 3u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, l)
                stack.setByte(2u, t)
                stack.setByte(3u, n)
            }

            0x25 -> {
                val stackret = stack.updateStackPointer(6u, 6u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, l2)
                stack.setShort(3u, t2)
                stack.setShort(5u, n2)
            }

            // DUP(2)
            0x06 -> {
                val stackret = stack.updateStackPointer(1u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, t)
                stack.setByte(2u, t)
            }

            0x26 -> {
                val stackret = stack.updateStackPointer(2u, 4u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, t2)
                stack.setShort(3u, t2)
            }

            // OVR(2)
            0x07 -> {
                val stackret = stack.updateStackPointer(2u, 3u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, n)
                stack.setByte(2u, t)
                stack.setByte(3u, n)
            }

            0x27 -> {
                val stackret = stack.updateStackPointer(4u, 6u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, n2)
                stack.setShort(3u, t2)
                stack.setShort(5u, n2)
            }
            // EQU(2)
            0x08 -> {
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, (n == t).into())
            }

            0x28 -> {
                val stackret = stack.updateStackPointer(4u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, (n2 == t2).into())
            }
            // NEQ(2)
            0x09 -> {
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, (n != t).into())
            }

            0x29 -> {
                val stackret = stack.updateStackPointer(4u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, (n2 != t2).into())
            }
            // GTH(2)
            0x0a -> {
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, (n > t).into())
            }

            0x2a -> {
                val stackret = stack.updateStackPointer(4u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, (n2 > t2).into())
            }

            // LTH(2)
            0x0b -> {
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, (n < t).into())
            }

            0x2b -> {
                val stackret = stack.updateStackPointer(4u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, (n2 < t2).into())
            }
            // JMP(2)
            0x0c -> {
                val stackret = stack.updateStackPointer(1u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                pc = (pc.toInt() + t.toByte()).toUShort() //TODO: WATCH THIS idk if I did it right
            }

            0x2c -> {
                val stackret = stack.updateStackPointer(2u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                pc = t2
            }
            // JCN(2)
            0x0d -> {
                val stackret = stack.updateStackPointer(2u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                if (n.toUInt() != 0u) {
                    pc = (pc.toInt() + t.toByte()).toUShort()
                }
            }
            0x2d -> {
                val stackret = stack.updateStackPointer(3u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                if (l.toUInt() != 0u) {
                    pc = t2
                }
            }
            // JSR(2)
            0x0e -> {
                val stackret = stack.updateStackPointer(1u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val stackret2 = rs.updateStackPointer(0u, 2u, false)
                if (stackret2.isPresent) {
                    return Either.right(stackret2.get())
                }

                rs.setShort(1u, pc)
                pc = (pc.toInt() + t.toByte()).toUShort() //TODO: WATCH I checked on kotlin playground and this looks *okay* but i am still unsure
            }

            0x2e -> {
                val stackret = stack.updateStackPointer(2u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val stackret2 = rs.updateStackPointer(0u, 2u, false)
                if (stackret2.isPresent) {
                    return Either.right(stackret2.get())
                }
                rs.setShort(1u, pc)
                pc = t2
            }
            // STH(2)
            0x0f -> {
                val stackret = stack.updateStackPointer(1u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val otherStack = if (returnMode) { ws } else { rs }
                val stackret2 = otherStack.updateStackPointer(0u, 1u, false)
                if (stackret2.isPresent) {
                    return Either.right(stackret2.get())
                }
                otherStack.setByte(1u, t)
            }

            0x2f -> {
                val stackret = stack.updateStackPointer(2u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val otherStack = if (returnMode) { ws } else { rs }
                val stackret2 = otherStack.updateStackPointer(0u, 2u, false)
                if (stackret2.isPresent) {
                    return Either.right(stackret2.get())
                }
                otherStack.setShort(1u, t2)
            }
            // LDZ(2)
            0x10 -> {
                val stackret = stack.updateStackPointer(1u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, ram[t.toInt()])
            }

            0x30 -> {
                val stackret = stack.updateStackPointer(1u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, ram[(t+1u).toInt()])
                stack.setByte(2u, ram[t.toInt()])
            }
            // STZ(2)
            0x11 -> {
                val stackret = stack.updateStackPointer(2u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                ram[t.toInt()] = n
            }

            0x31 -> {
                val stackret = stack.updateStackPointer(3u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                ram[ (t+1u).toInt()] = n
                ram[t.toInt()] = l
            }

            // LDR(2)
            0x12 -> {
                val stackret = stack.updateStackPointer(1u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(
                    1u,
                    ram[pc.toInt() + t.toByte()],
                )
            }

            0x32 -> {
                val stackret = stack.updateStackPointer(1u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(
                    1u,
                    ram[pc.toInt() + t.toByte() + 1]
                )
                stack.setByte(
                    2u,
                    ram[pc.toInt() + t.toByte()],
                )
            }

            // STR(2)
            0x13 -> {
                val stackret = stack.updateStackPointer(2u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                ram[pc.toInt() + t.toByte()] = n
            }

            0x33 -> {
                val stackret = stack.updateStackPointer(3u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                ram[pc.toInt() + t.toByte()] = l
                ram[pc.toInt() + t.toByte() +1] = n
            }

            // LDA(2)
            0x14 -> {
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, ram[t2.toInt()])
            }

            0x34 -> {
                val stackret = stack.updateStackPointer(2u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, ram[t2.toInt() +1 ])
                stack.setByte(2u, ram[t2.toInt()])
            }

            // STA(2)
            0x15 -> {
                val stackret = stack.updateStackPointer(3u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                ram[t2.toInt()] = l
            }

            0x35 -> {
                val stackret = stack.updateStackPointer(4u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }

                val value = n2.toUBytes()
                ram[t2.toInt()] = value.first
                ram[t2.toInt() + 1] = value.second
            }

            // DEI(2)
            0x16 -> {
                val stackret = stack.updateStackPointer(1u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val device = devices[t.and(0xf0u).rotateRight(4).toInt()]
                if (device.isPresent) {
                    stack.setByte(
                        1u,
                        device.get().readByte(t.and(0x0fu))
                    )
                } else {
                    stack.setByte(1u,0x00u)
                }
            }

            0x36 -> {
                val stackret = stack.updateStackPointer(1u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val device = devices[t.and(0xf0u).rotateRight(4).toInt()]
                if (device.isPresent) {
                    stack.setShort(1u, device.get().readShort(t.and(0x0fu)))
                } else {
                    stack.setShort(1u, 0x0000u)
                }
            }
            // DEO(2)
            0x17 -> {
                val stackret = stack.updateStackPointer(2u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val stackret2 = stack.updateStackPointer(1u, 1u, keepMode)
                if (stackret2.isPresent) {
                    return Either.right(stackret2.get())
                }
                val device = devices[t.and(0xf0u).rotateRight(4).toInt()]
                if (device.isPresent) {
                    device.get().writeByte(t.and(0x0fu), n)
                }
            }

            0x37 -> {
                val stackret = stack.updateStackPointer(3u, 0u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val device = devices[t.and(0xf0u).rotateRight(4).toInt()]
                if (device.isPresent) {
                    device.get().writeShort(t.and(0x0fu), h2)
                }
            }
            // ADD(2)
            0x18 -> {
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, (n+t).toUByte())
            }

            0x38 -> {
                val stackret = stack.updateStackPointer(4u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, (n2+t2).toUShort())
            }
            // SUB(2)
            0x19 -> {
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, (n +t).toUByte())
            }

            0x39 -> {
                val stackret = stack.updateStackPointer(4u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, (n2-t2).toUShort())
            }
            // MUL(2)
            0x1a -> {
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, (n*t).toUByte())
            }

            0x3a -> {
                val stackret = stack.updateStackPointer(4u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, (n2*t2).toUShort())
            }
            // DIV(2)
            0x1b -> {
                if (t.toUInt() == 0u) {return Either.right(UxnError.ZeroDiv)}
                val quotient = n / t
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, quotient.toUByte())
            }

            0x3b -> {
                if (t2.toUInt() == 0u) {return Either.right(UxnError.ZeroDiv)}
                val quotient = n2 / t2
                val stackret = stack.updateStackPointer(4u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, quotient.toUShort())
            }
            // AND(2)
            0x1c -> {
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, n.and(t))
            }

            0x3c -> {
                val stackret = stack.updateStackPointer(4u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, n2.and(t2))
            }

            // ORA(2)
            0x1d -> {
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, n.or(t))
            }

            0x3d -> {
                val stackret = stack.updateStackPointer(4u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, n2.or(t2))
            }

            // EOR(2)
            0x1e -> {
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, n.xor(t))
            }

            0x3e -> {
                val stackret = stack.updateStackPointer(4u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, n2.xor(t2))
            }

            // SFT(2)
            0x1f -> {
                val stackret = stack.updateStackPointer(2u, 1u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1u, 
                    n.rotateRight(t.and(0x0fu).toInt())
                        .rotateLeft(t.and(0xf0u).rotateRight(4).toInt())
                )
            }

            0x3f -> {
                val stackret = stack.updateStackPointer(3u, 2u, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1u, //TODO: CHECK THIS it may be implemented wrong
                        h2.rotateRight(t.and(0x0fu).toInt())
                            .rotateLeft(t.and(0xf0u).rotateRight(4).toInt())
                )
            }

            // Impossible.
            else -> {
                throw IllegalStateException(
                    "Reached the unreachable! instruction %s masked %s".format(
                        instruction.toHexString(),
                        maskedInstruction.toHexString()
                    )
                )
            }
        }

        return Either.left(false)
    }
}

fun UShort.toUBytes(): Pair<UByte,UByte> {
    return Pair(
        this.rotateRight(8).toUByte(),
        this.and(0xffu).toUByte()
    )
}
fun UByte.msbToUShort(lsb: UByte): UShort = (this.toUShort().rotateRight(8).or(lsb.toUShort()))
fun Boolean.into(): UByte {
    return (if (this) {
        1u
    } else {
        0u
    }).toUByte()
}
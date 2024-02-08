package net.walksanator.uxncraft.vm

abstract class Device(val uxn: Uxn) {
    abstract fun readByte(address: UByte): UByte;
    abstract fun writeByte(address: UByte, byte: UByte);
    abstract fun readShort(address: UByte): UShort;
    abstract fun writeShort(address: UByte, short: UShort);
}
package net.walksanator.uxncraft.vm

interface Device {
    fun readByte(address: UByte): UByte;
    fun writeByte(address: UByte, byte: UByte);
    fun readShort(address: UByte): UShort;
    fun writeShort(address: UByte, short: UShort);
}
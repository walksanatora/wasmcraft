data class Stack {
    val dat = ByteArray(0x100)
    var ptr: Uint8 = 0
}

class Uxn() {
    val ram = ByteArray(0x10000)
    val dev = ByteArray(0x100)
    val wst = Stack()
    val rst = Stack()
    var halted = false
    var fuel = 1000
}


package newhope

import chisel3._
import chisel3.util._
import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._

class NewHopeAccel(opcodes: OpcodeSet)(implicit p: Parameters) extends LazyRoCC(
    opcodes = opcodes, nPTWPorts = 0) {
    override lazy val module = new NewHopeAccelImp(this)
}

class NewHopeAccelImp(outer: NewHopeAccel)(implicit p: Parameters) extends LazyRoCCModuleImp(outer) {
  val cmd = Queue(io.cmd)
  val funct = cmd.bits.inst.funct
  
  // val shake_input = Wire(UInt(width = 32.W))
  // shake_input := 0.U

  val key_gen_true = RegInit(false.B)
  val key_gen_started = RegInit(false.B)
  val done = RegInit(false.B)
  when (funct === 0.U) {
    withClockAndReset(clock, reset) {
      printf("Function: %d\n", funct)
    }
    // cmd.ready := true.B
    // io.busy := true.B
    key_gen_true := true.B
    key_gen_started := true.B
  }
  .otherwise {
    // cmd.ready := false.B
    // io.busy := false.B
    key_gen_true := false.B
  }
  
  val KeyGenModule = KeyGen()
  KeyGenModule.io.start := key_gen_true

  when (key_gen_true && KeyGenModule.io.done) {
    withClockAndReset(clock, reset) {
      printf("Key Gen Module Done!\n")
    }
    withClockAndReset(clock, reset) {
      // printf("case 1: cmd.ready = false, io.busy = true\n")
    }
    done := true.B
    key_gen_true := false.B
    key_gen_started := false.B
    cmd.ready := false.B
    io.busy := true.B
  }
  .elsewhen (!done && key_gen_started) {
    withClockAndReset(clock, reset) {
      // printf("case 2: cmd.ready = false, io.busy = true\n")
    }
    cmd.ready := false.B
    io.busy := true.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      // printf("case 3: cmd.ready = false, io.busy = false\n")
    }
    cmd.ready := false.B
    io.busy := false.B
  }

  withClockAndReset(clock, reset) {
    printf("cmd.valid: %d\n", cmd.valid)
    printf("funct: %d\n", funct)
    // printf("Shake256Module out: 0x%x\n", Shake256Module.io.state_out)
  }

  // io.busy := cmd.valid
  io.interrupt := Bool(false)

  // MEMORY REQUEST INTERFACE
  // io.mem.req.valid := cmd.valid
  // io.mem.req.bits.addr := addr0 + (counterValue)
  // io.mem.req.bits.tag := (counter / 8.U)
  // io.mem.req.bits.cmd := memValue // perform a load (M_XWR for stores)
  // io.mem.req.bits.size := log2Ceil(8).U
  // io.mem.req.bits.signed := Bool(false)
  // io.mem.req.bits.data := 0.U // we're not performing any stores...
  // io.mem.req.bits.phys := Bool(false)

}

class WithNewHopeAccel extends Config ((site, here, up) => {
  case BuildRoCC => Seq(
    (p: Parameters) => {
      val newhope = LazyModule.apply(new NewHopeAccel(OpcodeSet.custom2)(p))
      newhope
    }
  )
})


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
  
  val public_key = RegInit(0.U((8*928).W))
  // val shake_input = Wire(UInt(width = 32.W))
  // shake_input := 0.U

  val key_gen_started = RegInit(false.B)
  // val key_gen_started = RegInit(false.B)
  val key_gen_finished = RegInit(false.B)
  val key_encaps_started = RegInit(false.B)
  val key_encaps_finished = RegInit(false.B)
  val done = RegInit(false.B)
  when (funct === 0.U) {
    withClockAndReset(clock, reset) {
      printf("Function: %d\n", funct)
    }
    // cmd.ready := true.B
    // io.busy := true.B
    key_gen_started := true.B
    // key_gen_started := true.B
  }
  .otherwise {
    // cmd.ready := false.B
    // io.busy := false.B
    key_gen_started := false.B
  }
  
  val KeyGenModule = KeyGen()
  KeyGenModule.io.start := key_gen_started
  
  val KeyEncapsModule = KeyEncaps()
  KeyEncapsModule.io.start := key_encaps_started
  KeyEncapsModule.io.public_key := public_key

  when (KeyGenModule.io.done && !key_gen_finished) {
    withClockAndReset(clock, reset) {
      printf("Key Gen Module Done!\n")
    }
    withClockAndReset(clock, reset) {
      printf("case 1\n")
      // printf("case 1: cmd.ready = false, io.busy = true\n")
    }
    public_key := KeyGenModule.io.public_key
    done := true.B
    key_gen_started := false.B
    key_gen_finished := true.B
    // key_encaps_started := true.B
    // key_encaps_started := true.B
    cmd.ready := false.B
    io.busy := true.B
  }
  .elsewhen (!key_gen_finished && key_gen_started) {
    withClockAndReset(clock, reset) {
      printf("case 2\n")
      // printf("case 1: cmd.ready = false, io.busy = true\n")
    }
  //  withClockAndReset(clock, reset) {
      // printf("case 2: cmd.ready = false, io.busy = true\n")
  //  }
    cmd.ready := false.B
    io.busy := true.B
  }
  .elsewhen (key_encaps_started && KeyEncapsModule.io.done) {
    withClockAndReset(clock, reset) {
      printf("case 4\n")
      // printf("case 1: cmd.ready = false, io.busy = true\n")
    }
    withClockAndReset(clock, reset) {
      printf("Key Encaps Module Done!\n")
    }
    key_encaps_started := false.B
    key_encaps_finished := true.B
    done := true.B
    cmd.ready := false.B
    io.busy := true.B

  }
  .elsewhen (!key_encaps_finished && key_encaps_started) {
    withClockAndReset(clock, reset) {
      printf("case 3\n")
      // printf("case 1: cmd.ready = false, io.busy = true\n")
    }
    cmd.ready := false.B
    io.busy := true.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("case 5\n")
      // printf("case 1: cmd.ready = false, io.busy = true\n")
    }
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


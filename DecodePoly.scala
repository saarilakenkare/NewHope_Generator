package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class DecodePoly extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val public_key = Input(UInt((8*928).W))
    val poly_out = Output(Vec(512, UInt(16.W)))
    val output_valid = Output(Bool())
  })

  val output_correct = RegInit(false.B)
   
  val QINV = 12287
  val Q = 12289

  val public_key = RegInit(VecInit(Seq.fill(928)(0.U(8.W))))
  val poly_out = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  
  val do_algo = RegInit(false.B)
  val key_index = RegInit(0.U(8.W))
  val key_out = Reg(Vec(896, UInt(8.W)))

  val do_initstep = RegInit(false.B)
  val do_aloop = RegInit(false.B)
  val do_aloop_init = RegInit(false.B)
  val do_aloop_for_s = RegInit(false.B)

  when (io.start && !do_algo) {
    do_algo := true.B
    do_initstep := true.B
    // public_key := io.public_key
    for (i <- 0 until 928) {
      public_key(i) := (io.public_key >> (8 * (927 - i))) & "hFF".U
    }
    withClockAndReset(clock, reset) {
      printf("DecodePoly public key input: 0x%x\n", Cat(io.public_key))
    }
  }

  when (do_algo) {
    withClockAndReset(clock, reset) {
      //printf("ext seed: 0x%x\n", Cat(ext_seed))
      //printf("do_initstep: %b\n", do_initstep)
      //printf("do_aloop: %b\n", do_aloop)
      //printf("do_aloop_init: %b\n", do_aloop_init)
      //printf("do_aloop_for_s: %b\n", do_aloop_for_s)
      //printf("do_aloop_while_ctr: %b\n", do_aloop_while_ctr)
    }

    withClockAndReset(clock, reset) {
      printf("DecodePoly vec public key: 0x%x\n", Cat(public_key))
    }
    for (i <- 0 until 512/4) {
      val t0  = (public_key(7*i+1) & "h3f".U) << 8
      val t1a = (public_key(7*i+2)) << 2
      val t1b = (public_key(7*i+3) & "h0f".U) << 10
      val t2a = (public_key(7*i+4)) << 4
      val t2b = (public_key(7*i+5) & "h03".U) << 12
      val t3  = (public_key(7*i+6)) << 6
      

      poly_out(4*i) := public_key(7*i) | t0
      poly_out(4*i+1) := (public_key(7*i+1) >> 6) | t1a | t1b
      poly_out(4*i+2) := (public_key(7*i+3) >> 4) | t2a | t2b
      poly_out(4*i+3) := (public_key(7*i+5) >> 2) | t3

    }
    output_correct := true.B
  }

  when (!output_correct) {
    io.poly_out := VecInit(Seq.fill(512)(0.U(16.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("Decode Poly Done: 0x%x\n", Cat(poly_out))
    }
    output_correct := false.B
    do_algo := false.B
    io.poly_out := poly_out
    io.output_valid := true.B
  }
}

object DecodePoly {
  def apply(): DecodePoly = Module(new DecodePoly())
}

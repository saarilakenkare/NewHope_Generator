package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class PolyNTT extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val poly_in = Input(Vec(512, UInt(16.W)))
    //val constant_in = Input(Vec(512, UInt(16.W)))
    val poly_out = Output(Vec(512, UInt(16.W)))
    val output_valid = Output(Bool())
  })

  val poly_s = RegInit(Vec(Seq.fill(512)(0.U(16.W))))
  val gammas_bitrev_montgomery = RegInit(Vec(Seq.fill(512)(0.U(16.W))))

  val do_algo = RegInit(false.B)
  val multiply_polys = RegInit(false.B)
  val ntt = RegInit(false.B)
  val output_correct = RegInit(false.B)
  

  val MultiplyPolysModule = MultiplyPolys()
  MultiplyPolysModule.io.start := multiply_polys
  MultiplyPolysModule.io.poly_in := poly_s
  MultiplyPolysModule.io.factors_in := Constants.gammas_bitrev_montgomery
  
  val NTTModule = NTT()
  NTTModule.io.start := ntt
  NTTModule.io.poly_in := poly_s
  NTTModule.io.powers_in := Constants.gammas_bitrev_montgomery
  
  when (io.start && !do_algo) {
    do_algo := true.B
    poly_s := io.poly_in
    multiply_polys := true.B
    // gammas_bitrev_montgomery := io.constant_in
    withClockAndReset(clock, reset) {
      printf("PolyNTT poly input: 0x%x\n", Cat(io.poly_in))
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

    when (MultiplyPolysModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("multiply polys message: 0x%x\n", Cat(MultiplyPolysModule.io.poly_out))
      }
      multiply_polys := false.B
      ntt := true.B
      poly_s := MultiplyPolysModule.io.poly_out
    }

    when (NTTModule.io.output_valid) {
      // poly_a := GenAModule.io.state_out
      withClockAndReset(clock, reset) {
        printf("ntt message: 0x%x\n", Cat(NTTModule.io.poly_out))
      }
      poly_s := NTTModule.io.poly_out
      output_correct := true.B
      ntt := false.B
    }

  }

  when (!output_correct) {
    io.poly_out := Vec(Seq.fill(512)(0.U(16.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("PolyNTT Done: 0x%x\n", Cat(poly_s))
    }
    output_correct := false.B
    do_algo := false.B
    io.poly_out := poly_s
    // io.state_out := Vec(Seq.fill(512)(0.U(16.W)))
    io.output_valid := true.B
  }

  // while (input_offset < io.length_in) {

  // }
  // when ((io.length_in - input_offset) < rate) {

  // }
}

object PolyNTT {
  def apply(): PolyNTT = Module(new PolyNTT())
}

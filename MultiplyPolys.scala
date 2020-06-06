package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class MultiplyPolys extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val poly_in = Input(Vec(512, UInt(16.W)))
    val factors_in = Input(Vec(512, UInt(16.W)))
    val poly_out = Output(Vec(512, UInt(16.W)))
    val output_valid = Output(Bool())
  })

  val output_correct = RegInit(false.B)
   
  val QINV = 12287
  val Q = 12289

  val poly_in = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val factors_in = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  
  val do_algo = RegInit(false.B)
  val poly_index = RegInit(0.U(8.W))
  val poly_out = Reg(Vec(512, UInt(16.W)))

  val do_initstep = RegInit(false.B)
  val do_aloop = RegInit(false.B)
  val do_aloop_init = RegInit(false.B)
  val do_aloop_for_s = RegInit(false.B)

  when (io.start && !do_algo) {
    do_algo := true.B
    do_initstep := true.B
    poly_in := io.poly_in
    factors_in := io.factors_in
    withClockAndReset(clock, reset) {
      printf("MultiplyPolys poly input: 0x%x\n", Cat(io.poly_in))
      printf("MultiplyPolys factors input: 0x%x\n", Cat(io.factors_in))
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

    for (i <- 0 until 512) {
      val a = poly_in(i)
      val b = factors_in(i)

      val mul = a * b
      
      val u = Wire(UInt(32.W))
      val r = Wire(UInt(32.W))
      val v = Wire(UInt(32.W))
      val w = Wire(UInt(32.W))
      val s = Wire(UInt(32.W))
      val mont_reduction = Wire(UInt(32.W))

      u := mul * QINV.U
      r := 1.U << 18
      v := u & (r - 1.U)
      w := v * Q.U
      s := mul + w
      mont_reduction := s >> 18
      poly_out(i) := mont_reduction
    }
    output_correct := true.B
  }

  when (!output_correct) {
    io.poly_out := VecInit(Seq.fill(512)(0.U(16.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("Multiply Polys Done: 0x%x\n", Cat(poly_out))
    }
    output_correct := false.B
    do_algo := false.B
    io.poly_out := poly_out
    // io.state_out := Vec(Seq.fill(512)(0.U(16.W)))
    io.output_valid := true.B
  }

  // while (input_offset < io.length_in) {

  // }
  // when ((io.length_in - input_offset) < rate) {

  // }
}

object MultiplyPolys {
  def apply(): MultiplyPolys = Module(new MultiplyPolys())
}

package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class SubtractPolys extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val poly_a_in = Input(Vec(512, UInt(16.W)))
    val poly_b_in = Input(Vec(512, UInt(16.W)))
    val poly_out = Output(Vec(512, UInt(16.W)))
    val output_valid = Output(Bool())
  })

  val output_correct = RegInit(false.B)
   
  val QINV = 12287
  val Q = 12289

  val poly_a_in = RegInit(Vec(Seq.fill(512)(0.U(16.W))))
  val poly_b_in = RegInit(Vec(Seq.fill(512)(0.U(16.W))))
  
  val do_algo = RegInit(false.B)
  val poly_out = Reg(Vec(512, UInt(16.W)))

  val do_initstep = RegInit(false.B)
  val do_aloop = RegInit(false.B)
  val do_aloop_init = RegInit(false.B)
  val do_aloop_for_s = RegInit(false.B)

  when (io.start && !do_algo) {
    do_algo := true.B
    do_initstep := true.B
    poly_a_in := io.poly_a_in
    poly_b_in := io.poly_b_in
    withClockAndReset(clock, reset) {
      printf("SubtractPolys poly a input: 0x%x\n", Cat(io.poly_a_in))
      printf("SubtractPolys poly b input: 0x%x\n", Cat(io.poly_b_in))
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
      val a = poly_a_in(i)
      val b = poly_b_in(i)

      withClockAndReset(clock, reset) {
        //printf("a: 0x%x, b: 0x%x\n", a, b)
      }

      poly_out(i) := (a + 3.U*Q.U - b) % Q.U
    }
    output_correct := true.B
  }

  when (!output_correct) {
    io.poly_out := Vec(Seq.fill(512)(0.U(16.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("Subtract Polys Done: 0x%x\n", Cat(poly_out))
    }
    output_correct := false.B
    do_algo := false.B
    io.poly_out := poly_out
    io.output_valid := true.B
  }
}

object SubtractPolys {
  def apply(): SubtractPolys = Module(new SubtractPolys())
}

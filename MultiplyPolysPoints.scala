package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class MultiplyPolysPoints extends Module {
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

  val poly_a_in = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_b_in = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  
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
    poly_a_in := io.poly_a_in
    poly_b_in := io.poly_b_in
    withClockAndReset(clock, reset) {
      printf("MultiplyPolysPoints poly a input: 0x%x\n", Cat(io.poly_a_in))
      printf("MultiplyPolysPoints poly b input: 0x%x\n", Cat(io.poly_b_in))
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

      val mul_1 = 3186.U * b
      
      val u_1 = Wire(UInt(32.W))
      val r_1 = Wire(UInt(32.W))
      val v_1 = Wire(UInt(32.W))
      val w_1 = Wire(UInt(32.W))
      val s_1 = Wire(UInt(32.W))
      val t = Wire(UInt(32.W))

      u_1 := mul_1 * QINV.U
      r_1 := 1.U << 18
      v_1 := u_1 & (r_1 - 1.U)
      w_1 := v_1 * Q.U
      s_1 := mul_1 + w_1
      t := s_1 >> 18

      withClockAndReset(clock, reset) {
        //printf("t: 0x%x\n", t)
      }
      
      val mul_2 = a * t

      val u_2 = Wire(UInt(32.W))
      val r_2 = Wire(UInt(32.W))
      val v_2 = Wire(UInt(32.W))
      val w_2 = Wire(UInt(32.W))
      val s_2 = Wire(UInt(32.W))
      val mont_reduction = Wire(UInt(32.W))

      u_2 := mul_2 * QINV.U
      r_2 := 1.U << 18
      v_2 := u_2 & (r_2 - 1.U)
      w_2 := v_2 * Q.U
      s_2 := mul_2 + w_2
      mont_reduction := s_2 >> 18
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
      printf("Multiply Polys Points Done: 0x%x\n", Cat(poly_out))
    }
    output_correct := false.B
    do_algo := false.B
    io.poly_out := poly_out
    io.output_valid := true.B
  }
}

object MultiplyPolysPoints {
  def apply(): MultiplyPolysPoints = Module(new MultiplyPolysPoints())
}

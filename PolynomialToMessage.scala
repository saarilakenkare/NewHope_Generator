package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class PolynomialToMessage extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val poly_in = Input(Vec(512, UInt(16.W)))
    // val message_out = Output(Vec(32, UInt(8.W)))
    val message_out = Output(UInt(256.W))
    val output_valid = Output(Bool())
  })

  val Q = 12289
  
  val output_correct = RegInit(false.B)
  val output_index = RegInit(0.U(8.W))
  val output_reg = RegInit(0.U(1600.W))

  // val message_out = RegInit(Vec(Seq.fill(32)(0.U(8.W))))
  val message_out = RegInit(0.U(256.W))

  val poly_index = RegInit(0.U(8.W))
  val poly_in = Reg(Vec(512, UInt(16.W)))

  val do_algo = RegInit(false.B)
  val do_initstep = RegInit(false.B)
  val input = RegInit(0.U(512.W))

  when (io.start && !do_algo) {
    do_algo := true.B
    do_initstep := true.B
    // do_loop := true.B
    // do_loop_init := true.B
    poly_in := io.poly_in
    withClockAndReset(clock, reset) {
      printf("P2M input: 0x%x\n", Cat(io.poly_in))
    }
  }
  when (do_algo) {
    output_reg := 0.U
    withClockAndReset(clock, reset) {
      //printf("ext seed: 0x%x\n", Cat(ext_seed))
      //printf("do_initstep: %b\n", do_initstep)
      //printf("do_aloop: %b\n", do_aloop)
      //printf("do_aloop_init: %b\n", do_aloop_init)
      //printf("do_aloop_for_s: %b\n", do_aloop_for_s)
      //printf("do_aloop_while_ctr: %b\n", do_aloop_while_ctr)
    }

    val array = Wire(Vec(256, UInt(1.W)))
    for (i <- 0 until 256) {
      array(i) := 0.U
    }

    for (i <- 0 until 256) {
      val t1 = poly_in(i)
      val t2 = poly_in(i+256)
        
      val r1 = Wire(UInt(16.W))
      val m1 = Wire(UInt(16.W))
      val c1 = Wire(UInt(16.W))
      val f1 = Wire(UInt(16.W))
      val x1 = Wire(UInt(16.W))
      val z1 = Wire(UInt(16.W))

      val r2 = Wire(UInt(16.W))
      val m2 = Wire(UInt(16.W))
      val c2 = Wire(UInt(16.W))
      val f2 = Wire(UInt(16.W))
      val x2 = Wire(UInt(16.W))
      val z2 = Wire(UInt(16.W))

      r1 := t1 % Q.U
      m1 := r1 - Q.U
      c1 := "hFFFFFFFF".U // m0 >> 15
      f1 := (m1 ^ ((r1^m1) & c1))
      // x1 := "hFFFFFFFF".U // m0 >> 15
      when ( f1 > (Q.U/2.U)) {
        x1 := 0.U
      }
      .otherwise {
        x1 := "hFFFFFFFF".U
      } 
      z1 := ((f1 - Q.U/2.U) + x1) ^ x1
      
      r2 := t2 % Q.U
      m2 := r2 - Q.U
      c2 := "hFFFFFFFF".U // m0 >> 15
      f2 := (m2 ^ ((r2^m2) & c2))
      x2 := "hFFFFFFFF".U // m0 >> 15
      when ( f2 > (Q.U/2.U)) {
        x2 := 0.U
      }
      .otherwise {
        x2 := "hFFFFFFFF".U
      } 
      z2 := ((f2 - Q.U/2.U) + x2) ^ x2

      val t = ((z1 + z2) - (Q.U/2.U)) >> 15

      // message_out(i>>3) := message_out(i>>3) | (t << (i & 7))
      when (t === 1.U) {
        array( 8*(i/8) + (7 - (i% 8))) := 1.U
      }

      withClockAndReset(clock, reset) {
        printf("poly[%d]: 0x%x; poly[%d]: 0x%x\n", i.U, poly_in(i), (i+256).U, poly_in(i+256))
        printf("r1: 0x%x; m1: 0x%x; f1: 0x%x, x1: 0x%x\n", r1, m1, f1, x1)
        printf("flip_abs(i): 0x%x; flip_abs(i+256): 0x%x\n", z1, z2)
        printf("z1 + z2: 0x%x\n", z1 + z2)
        printf("t: 0x%x\n", t)
      }   
    }

    message_out := Cat(array)
    output_correct := true.B
    withClockAndReset(clock, reset) {
      printf("done with m2p loop\n")
    } 
  }

  when (!output_correct) {
    io.message_out := 0.U
    // io.message_out := Vec(Seq.fill(32)(0.U(8.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("P2M Done: 0x%x\n", message_out)
    }
    output_correct := false.B
    do_algo := false.B
    io.message_out := message_out
    // io.state_out := Vec(Seq.fill(512)(0.U(16.W)))
    io.output_valid := true.B
  }

  // while (input_offset < io.length_in) {

  // }
  // when ((io.length_in - input_offset) < rate) {

  // }
}

object PolynomialToMessage {
  def apply(): PolynomialToMessage = Module(new PolynomialToMessage())
}

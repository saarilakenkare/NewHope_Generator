package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class SerializePoly extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val poly_in = Input(Vec(512, UInt(16.W)))
    val key_out = Output(Vec(896, UInt(8.W)))
    val output_valid = Output(Bool())
  })

  val output_correct = RegInit(false.B)
   
  val QINV = 12287
  val Q = 12289

  val poly_in = RegInit(Vec(Seq.fill(512)(0.U(16.W))))
  
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
    poly_in := io.poly_in
    withClockAndReset(clock, reset) {
      printf("SerializePoly poly input: 0x%x\n", Cat(io.poly_in))
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

    for (i <- 0 until 512/4) {
      val t0 = poly_in(4*i+0)
      val t1 = poly_in(4*i+1)
      val t2 = poly_in(4*i+2)
      val t3 = poly_in(4*i+3)

      val r0 = Wire(UInt(16.W))
      val r1 = Wire(UInt(16.W))
      val r2 = Wire(UInt(16.W))
      val r3 = Wire(UInt(16.W))
      val m0 = Wire(UInt(16.W))
      val m1 = Wire(UInt(16.W))
      val m2 = Wire(UInt(16.W))
      val m3 = Wire(UInt(16.W))
      val c0 = Wire(UInt(16.W))
      val c1 = Wire(UInt(16.W))
      val c2 = Wire(UInt(16.W))
      val c3 = Wire(UInt(16.W))
      val f0 = Wire(UInt(16.W))
      val f1 = Wire(UInt(16.W))
      val f2 = Wire(UInt(16.W))
      val f3 = Wire(UInt(16.W))

      r0 := t0 % Q.U
      r1 := t1 % Q.U
      r2 := t2 % Q.U
      r3 := t3 % Q.U

      m0 := r0 - Q.U
      m1 := r1 - Q.U
      m2 := r2 - Q.U
      m3 := r3 - Q.U

      c0 := "hFFFFFFFF".U // m0 >> 15
      c1 := "hFFFFFFFF".U // m1 >> 15
      c2 := "hFFFFFFFF".U // m2 >> 15
      c3 := "hFFFFFFFF".U // m3 >> 15

      f0 := m0 ^ ((r0^m0) & c0)
      f1 := m1 ^ ((r1^m1) & c1)
      f2 := m2 ^ ((r2^m2) & c2)
      f3 := m3 ^ ((r3^m3) & c3)

      withClockAndReset(clock, reset) {
          // printf("p0: 0x%x, p1: 0x%x, p2: 0x%x, p3: 0x%x\n", t0, t1, t2, t3)
          printf("r0: 0x%x, r1: 0x%x, r2: 0x%x, r3: 0x%x\n", r0, r1, r2, r3)
          printf("m0: 0x%x, m1: 0x%x, m2: 0x%x, m3: 0x%x\n", m0, m1, m2, m3)
          printf("c0: 0x%x, c1: 0x%x, c2: 0x%x, c3: 0x%x\n", c0, c1, c2, c3)
          printf("t0: 0x%x, t1: 0x%x, t2: 0x%x, t3: 0x%x\n", f0, f1, f2, f3)
      }

      key_out(7*i+0) := f0 & "hFF".U
      key_out(7*i+1) := (f0 >> 8) | (f1 << 6)
      key_out(7*i+2) := f1 >> 2
      key_out(7*i+3) := (f1 >> 10) | (f2 << 4)
      key_out(7*i+4) := f2 >> 4
      key_out(7*i+5) := (f2 >> 12) | (f3 << 2)
      key_out(7*i+6) := f3 >> 6


      withClockAndReset(clock, reset) {
        //printf("a: 0x%x, b: 0x%x\n", a, b)
      }

    }
    output_correct := true.B
  }

  when (!output_correct) {
    io.key_out := Vec(Seq.fill(896)(0.U(8.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("Serialize Poly Done: 0x%x\n", Cat(key_out))
    }
    output_correct := false.B
    do_algo := false.B
    io.key_out := key_out
    io.output_valid := true.B
  }
}

object SerializePoly {
  def apply(): SerializePoly = Module(new SerializePoly())
}

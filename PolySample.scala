package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class PolySample extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    // val poly_in = Input(Vec(512, UInt(16.W)))
    val seed_in = Input(UInt(256.W))
    val byte_in = Input(UInt(8.W))
    val state_out = Output(Vec(512, UInt(16.W)))
    val output_valid = Output(Bool())
  })

  val output_correct = RegInit(false.B)
  val output_index = RegInit(0.U(8.W))
  val output_reg = RegInit(0.U(1600.W))

  val Q = 12289

  val input = RegInit(0.U(256.W))
  val byte = RegInit(0.U(8.W))

  val do_algo = RegInit(false.B)
  val index = RegInit(0.U(4.W))
  val poly_index = RegInit(0.U(8.W))
  val poly_out = Reg(Vec(512, UInt(16.W)))

  val do_initstep = RegInit(false.B)
  val do_loop = RegInit(false.B)
  val do_loop_init = RegInit(false.B)
  val do_loop_start_shake = RegInit(false.B)

  val ext_seed = RegInit(VecInit(Seq.fill(34)(0.U(8.W))))


  val Shake256Module = Shake256()
  Shake256Module.io.start := false.B
  Shake256Module.io.state_in := 0.U
  Shake256Module.io.length_in := 34.U
  Shake256Module.io.length_out := 128.U

  when (io.start && !do_algo) {
    do_algo := true.B
    do_initstep := true.B
    input := io.seed_in
    byte := io.byte_in
    index := 0.U
    withClockAndReset(clock, reset) {
      printf("PolySample input: 0x%x\n", Cat(io.seed_in))
      printf("PolySample byte: 0x%x\n", io.byte_in)
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
    when (do_initstep) {
      for (i <- 0 until 32) {
        ext_seed(i) := input >> ((31 - i) * 8)
      }
      ext_seed(32) := byte
      do_initstep := false.B
      do_loop := true.B
      do_loop_init := true.B
    }

    when (do_loop) {
      when (do_loop_init) {
        ext_seed(33) := index
        do_loop_init := false.B
        do_loop_start_shake := true.B
        withClockAndReset(clock, reset) {
          //printf("s: 0x%x\n", Cat(s))
          //printf("t: 0x%x\n", Cat(t))
        }
      }

      when (do_loop_start_shake) {
        withClockAndReset(clock, reset) {
          // printf("aloop_while_ctr a: %d, bytes: 0x%x\n", a, Cat(bytes))
          printf("PolySample starting Shake, input: 0x%x\n", Cat(ext_seed))
        }
        // val cur_bytes = Vec(Seq.fill(200)(0.U(8.W)))
        Shake256Module.io.start := true.B
        Shake256Module.io.state_in := Cat(ext_seed)
        do_loop_start_shake := false.B
      }
      .otherwise {
        Shake256Module.io.start := false.B
      }

      when (Shake256Module.io.output_valid) {
          withClockAndReset(clock, reset) {
            printf("PolySample/Shake256 out: 0x%x\n", Shake256Module.io.state_out)
          }
          val shake = Shake256Module.io.state_out
          for (i <- 0 until 64) {
            val a = Wire(UInt(8.W))
            a := (shake >> (8 * (127 - 2 * i))) & "hFF".U
            val b = Wire(UInt(8.W))
            b := (shake >> (8 * (126 - 2 * i))) & "hFF".U

            withClockAndReset(clock, reset) {
              // printf("i: %d, a: 0x%x, b: 0x%x\n", i.U, a, b)
              // printf("i: %d, a(0): 0x%x, b(0): 0x%x\n", i.U, a(0), b(0))
              // printf("i: %d, a(1): 0x%x, b(1): 0x%x\n", i.U, a(1), b(1))
              // printf("i: %d, a(2): 0x%x, b(2): 0x%x\n", i.U, a(2), b(2))
              // printf("i: %d, a(3): 0x%x, b(3): 0x%x\n", i.U, a(3), b(3))
              // printf("i: %d, a(0-1): 0x%x, a(0-2): 0x%x, a(0-3): 0x%x\n", i.U, a(0) + a(1) + a(2) + a(3), a(0) + a(1) + a(2) + a(3) + a(4) + a(5), a(0) + a(1) + a(2) + a(3) + a(4) + a(5) + a(6) + a(7))
            }

            val base = Wire(UInt(4.W))
            base := 0.U
            val hamming_a = Wire(UInt(4.W))
            val hamming_b = Wire(UInt(4.W))
            
            hamming_a := base + a(0) + a(1) + a(2) + a(3) + a(4) + a(5) + a(6) + a(7)
            hamming_b := base + b(0) + b(1) + b(2) + b(3) + b(4) + b(5) + b(6) + b(7)
            
            withClockAndReset(clock, reset) {
              // printf("i: %d, h_a: 0x%x, h_b: 0x%x\n", i.U, hamming_a, hamming_b)
              printf("poly_sample[%d]: 0x%x\n", 64.U * index + i.U, hamming_a + Q.U - hamming_b)
            }

            poly_out(64.U * index + i.U) := hamming_a + Q.U - hamming_b
            // bytes(i) := spm_out >> (8 * (199 - i))
          }
          //poly_index := 0.U
          //do_aloop_while_ctr := false.B
          //do_aloop_fill_poly := true.B
          when (index === 7.U) {
            output_correct := true.B
          }
          .otherwise {
            index := index + 1.U
            do_loop_init := true.B
          }
        
      }

    }
  }

  when (!output_correct) {
    io.state_out := VecInit(Seq.fill(512)(0.U(16.W)))
    io.output_valid := false.B
  }
  .otherwise {
    output_correct := false.B
    withClockAndReset(clock, reset) {
      printf("Poly Sample Sending: 0x%x\n", Cat(poly_out))
    }
    do_algo := false.B
    io.state_out := poly_out
    // io.state_out := Vec(Seq.fill(512)(0.U(16.W)))
    io.output_valid := true.B
  }

  // while (input_offset < io.length_in) {

  // }
  // when ((io.length_in - input_offset) < rate) {

  // }
}

object PolySample {
  def apply(): PolySample = Module(new PolySample())
}

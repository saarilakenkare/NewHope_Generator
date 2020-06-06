package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class CompressPoly extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val poly_in = Input(Vec(512, UInt(16.W)))
    val cipher_out = Output(Vec(192, UInt(8.W)))
    val output_valid = Output(Bool())
  })

  val output_correct = RegInit(false.B)
   
  val QINV = 12287
  val Q = 12289

  val poly_in = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  
  val do_algo = RegInit(false.B)
  val cipher_index = RegInit(0.U(8.W))
  val cipher_out = Reg(Vec(192, UInt(8.W)))

  when (io.start && !do_algo) {
    do_algo := true.B
    poly_in := io.poly_in
    withClockAndReset(clock, reset) {
      printf("CompressPoly poly input: 0x%x\n", Cat(io.poly_in))
    }
  }

  when (do_algo) {
    withClockAndReset(clock, reset) {
      //printf("ext seed: 0x%x\n", Cat(ext_seed))
      //printf("do_aloop: %b\n", do_aloop)
      //printf("do_aloop_init: %b\n", do_aloop_init)
      //printf("do_aloop_for_s: %b\n", do_aloop_for_s)
      //printf("do_aloop_while_ctr: %b\n", do_aloop_while_ctr)
    }

    val k = Wire(UInt(32.W))
    k := 0.U
    for (i <- 0 until 512 by 8) {
      val array = VecInit(Seq.fill(8)(0.U(32.W)))
      withClockAndReset(clock, reset) {
        printf("i1: %d\n", ((i/8)*3).U)
      }
      for (j <- 0 until 8) {
        val t = poly_in(i+j)

        val r = Wire(UInt(16.W))
        val m = Wire(UInt(16.W))
        val c = Wire(UInt(16.W))
        val f = Wire(UInt(16.W))

        r := t % Q.U
        m := r - Q.U
        c := "hFFFFFFFF".U // m0 >> 15
        f := m ^ ((r^m) & c)
      
        array(j) := (((f << 3.U) + Q.U/2.U)/Q.U) & "h7".U
        withClockAndReset(clock, reset) {
          printf("f: 0x%x\n", f)
          printf("array(%d): 0x%x\n", j.U, array(j))
        }
      }

        //withClockAndReset(clock, reset) {
          //printf("r0: 0x%x, r1: 0x%x, r2: 0x%x, r3: 0x%x\n", r0, r1, r2, r3)
          //printf("m0: 0x%x, m1: 0x%x, m2: 0x%x, m3: 0x%x\n", m0, m1, m2, m3)
          //printf("c0: 0x%x, c1: 0x%x, c2: 0x%x, c3: 0x%x\n", c0, c1, c2, c3)
          //printf("t0: 0x%x, t1: 0x%x, t2: 0x%x, t3: 0x%x\n", f0, f1, f2, f3)
        //}

      cipher_out((i/8)*3)   := array(0) | (array(1) << 3) | (array(2) << 6)
      cipher_out((i/8)*3+1) := (array(2) >> 2) | (array(3) << 1) | (array(4) << 4) | (array(5) << 7)
      cipher_out((i/8)*3+2) := (array(5) >> 1) | (array(6) << 2) | (array(7) << 5)

      // k := k + 3.U


    }
    output_correct := true.B
  }

  when (!output_correct) {
    io.cipher_out := VecInit(Seq.fill(192)(0.U(8.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("Cipher Poly Done: 0x%x\n", Cat(cipher_out))
    }
    output_correct := false.B
    do_algo := false.B
    io.cipher_out := cipher_out
    io.output_valid := true.B
  }
}

object CompressPoly {
  def apply(): CompressPoly = Module(new CompressPoly())
}

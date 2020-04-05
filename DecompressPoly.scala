package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class DecompressPoly extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val cipher_in = Input(UInt((192*8).W))
    val poly_out = Output(Vec(512, UInt(16.W)))
    val output_valid = Output(Bool())
  })

  val output_correct = RegInit(false.B)
   
  val QINV = 12287
  val Q = 12289

  val poly_out = RegInit(Vec(Seq.fill(512)(0.U(16.W))))
  
  val do_algo = RegInit(false.B)
  val cipher_index = RegInit(0.U(8.W))
  val cipher_in = Reg(Vec(192, UInt(8.W)))

  when (io.start && !do_algo) {
    do_algo := true.B
    for (i <- 0 until 192) {
      cipher_in(i) := (io.cipher_in >> (8 * (191 - i))) & "hFF".U
    }
    withClockAndReset(clock, reset) {
      printf("DecompressPoly cipher input: 0x%x\n", Cat(io.cipher_in))
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

    withClockAndReset(clock, reset) {
      printf("DecompressPoly vec cipher: 0x%x\n", Cat(cipher_in))
    }
    for (idx <- 0 until 512 by 8) {
      val array = Wire(Vec(8, UInt(32.W)))
      val i = 3 * (idx/8)
      array(0) := cipher_in(i) & "h7".U
      array(1) := (cipher_in(i) >> 3) & 7.U
      array(2) := (cipher_in(i) >> 6) | ((cipher_in(i+1) << 2) & 4.U)
      array(3) := (cipher_in(i+1) >> 1) & 7.U
      array(4) := (cipher_in(i+1) >> 4) & 7.U
      array(5) := (cipher_in(i+1) >> 7) | ((cipher_in(i+2) << 1) & 6.U)
      array(6) := (cipher_in(i+2) >> 2) & 7.U
      array(7) := (cipher_in(i+2) >> 5) 

      for (j <- 0 until 8) {
        poly_out(idx+j) := (array(j) * Q.U + 4.U) >> 3
      }

      withClockAndReset(clock, reset) {
        printf("idx: %d, i: %d, cipher(i): 0x%x, cipher(i) & 7 : 0x%x\n", idx.U, i.U, cipher_in(i), cipher_in(i) & "h7".U)
        for (j <- 0 until 8) {
          printf("array(%d) = 0x%x\n", j.U, array(j))
        }
      }
    }
    output_correct := true.B
  }

  when (!output_correct) {
    io.poly_out := Vec(Seq.fill(512)(0.U(16.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("Decompress Poly Done: 0x%x\n", Cat(poly_out))
    }
    output_correct := false.B
    do_algo := false.B
    io.poly_out := poly_out
    io.output_valid := true.B
  }
}

object DecompressPoly {
  def apply(): DecompressPoly = Module(new DecompressPoly())
}

package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class EncodeCipher extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val poly1_in = Input(Vec(512, UInt(16.W)))
    val poly2_in = Input(Vec(512, UInt(16.W)))
    val cipher_out = Output(Vec(1088, UInt(8.W)))
    val output_valid = Output(Bool())
  })

  val poly1_in = RegInit(Vec(Seq.fill(512)(0.U(16.W))))
  val poly2_in = RegInit(Vec(Seq.fill(512)(0.U(16.W))))
  val cipher_out = RegInit(Vec(Seq.fill(1088)(0.U(8.W))))

  val do_algo = RegInit(false.B)
  val serialize_poly = RegInit(false.B)
  val compress_poly = RegInit(false.B)
  val done1 = RegInit(false.B)
  val done2 = RegInit(false.B)
  val output_correct = RegInit(false.B)
  
  val SerializePolyModule = SerializePoly()
  SerializePolyModule.io.start := serialize_poly
  SerializePolyModule.io.poly_in := poly1_in
  
  val CompressPolyModule = CompressPoly()
  CompressPolyModule.io.start := compress_poly
  CompressPolyModule.io.poly_in := poly2_in
  
  when (io.start && !do_algo) {
    do_algo := true.B
    poly1_in := io.poly1_in
    poly2_in := io.poly2_in
    // multiply_polys := true.B
    // reverse_poly := true.B
    // gammas_bitrev_montgomery := io.constant_in
    serialize_poly := true.B
    compress_poly := true.B
    withClockAndReset(clock, reset) {
      printf("EncodeCipher poly1 input: 0x%x\n", Cat(io.poly1_in))
      printf("EncodeCipher poly2 input: 0x%x\n", Cat(io.poly2_in))
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

    when (SerializePolyModule.io.output_valid) {
      serialize_poly := false.B
      // poly_a := GenAModule.io.state_out
      withClockAndReset(clock, reset) {
        printf("serialize message: 0x%x\n", Cat(SerializePolyModule.io.key_out))
      }
      for (i <- 0 until 896) {
        cipher_out(i) := SerializePolyModule.io.key_out(i)
      }  
      done1 := true.B
    }

    when (CompressPolyModule.io.output_valid) {
      // poly_a := GenAModule.io.state_out
      compress_poly := false.B
      withClockAndReset(clock, reset) {
        printf("compress message: 0x%x\n", Cat(CompressPolyModule.io.cipher_out))
      }
      for (i <- 0 until 192) {
        cipher_out(i+896) := CompressPolyModule.io.cipher_out(i)
      }  
      done2 := true.B
    }

  }

  output_correct := done1 && done2
  when (!output_correct) {
    io.cipher_out := Vec(Seq.fill(1088)(0.U(8.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("EncodeCipher Done: 0x%x\n", cipher_out(0))
    }
    done1 := false.B
    done2 := false.B
    output_correct := false.B
    do_algo := false.B
    io.cipher_out := cipher_out
    // io.state_out := Vec(Seq.fill(512)(0.U(16.W)))
    io.output_valid := true.B
  }

  // while (input_offset < io.length_in) {

  // }
  // when ((io.length_in - input_offset) < rate) {

  // }
}

object EncodeCipher {
  def apply(): EncodeCipher = Module(new EncodeCipher())
}

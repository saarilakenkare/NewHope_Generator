package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class SerializePublicKey extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val poly_in = Input(Vec(512, UInt(16.W)))
    val seed_in = Input(UInt(256.W))
    val key_out = Output(Vec(928, UInt(8.W)))
    val output_valid = Output(Bool())
  })

  val output_correct = RegInit(false.B)
  
  val poly_in = RegInit(Vec(Seq.fill(512)(0.U(16.W))))
  // val seed_in = RegInit(Vec(Seq.fill(32)(0.U(8.W))))
 

  val do_algo = RegInit(false.B)
  val key_index = RegInit(0.U(8.W))
  val key_out = Reg(Vec(928, UInt(8.W)))

  val do_initstep = RegInit(false.B)
  val do_serialize_poly = RegInit(false.B)

  val SerializePolyModule = SerializePoly()
  SerializePolyModule.io.start := do_serialize_poly
  SerializePolyModule.io.poly_in := poly_in

  when (io.start && !do_algo) {
    // do_algo := true.B
    // do_initstep := true.B
    do_serialize_poly := true.B
    poly_in := io.poly_in
    // seed_in := io.seed_in
    for (i <- 0 until 32) {
      key_out(896 + i) := (io.seed_in >> (8 * (31 - i))) & "hFF".U
    }
    withClockAndReset(clock, reset) {
      printf("SerializePublicKey poly input: 0x%x\n", Cat(io.poly_in))
      printf("SerializePublicKey seed input: 0x%x\n", Cat(io.seed_in))
    }
  }

  when (do_serialize_poly) {
    do_serialize_poly := false.B
  }

  when (SerializePolyModule.io.output_valid) {
    for (i <- 0 until 896) {
      key_out(i) := SerializePolyModule.io.key_out(i)
    }
    output_correct := true.B
  }

  //when (do_algo) {
  //  withClockAndReset(clock, reset) {
      //printf("ext seed: 0x%x\n", Cat(ext_seed))
      //printf("do_initstep: %b\n", do_initstep)
      //printf("do_aloop: %b\n", do_aloop)
      //printf("do_aloop_init: %b\n", do_aloop_init)
      //printf("do_aloop_for_s: %b\n", do_aloop_for_s)
      //printf("do_aloop_while_ctr: %b\n", do_aloop_while_ctr)
  //  }

  //  output_correct := true.B
  //}

  when (!output_correct) {
    io.key_out := Vec(Seq.fill(928)(0.U(8.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("Serialize Public Key Done: 0x%x\n", Cat(key_out))
    }
    output_correct := false.B
    // do_algo := false.B
    io.key_out := key_out
    io.output_valid := true.B
  }
}

object SerializePublicKey {
  def apply(): SerializePublicKey = Module(new SerializePublicKey())
}

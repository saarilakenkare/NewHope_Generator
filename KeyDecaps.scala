package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class KeyDecaps extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val ciphertext = Input(UInt((8*1088).W))
    val secret = Input(UInt((8*896).W))
    val done = Output(Bool())
  })

  val shake_256_true = RegInit(false.B)
  val decode_pk_true_1 = RegInit(false.B)
  val decode_pk_true_2 = RegInit(false.B)
  val poly_multiply_points_true = RegInit(false.B)
  val poly_inv_ntt_true = RegInit(false.B)
  val decompress_poly_true = RegInit(false.B)
  val subtract_polys_true = RegInit(false.B)
  val polynomial_to_message_true = RegInit(false.B)

  val ciphertext = RegInit(0.U((8*1088).W))
  val decompress_ciphertext = RegInit(0.U((8*192).W))
  val secret = RegInit(0.U((8*896).W))
  val message = RegInit(0.U(512.W))
  
  val poly_u = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_v = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_s = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_tmp = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  
  val DecodePolyModule1 = DecodePoly()
  DecodePolyModule1.io.start := decode_pk_true_1
  DecodePolyModule1.io.public_key := secret << (8 * (928 - 896))

  val DecodePolyModule2 = DecodePoly()
  DecodePolyModule2.io.start := decode_pk_true_2
  DecodePolyModule2.io.public_key := ciphertext >> (8 * (1088 - 928))

  val MultiplyPolysPointsModule = MultiplyPolysPoints()
  MultiplyPolysPointsModule.io.start := poly_multiply_points_true
  MultiplyPolysPointsModule.io.poly_a_in := poly_s
  MultiplyPolysPointsModule.io.poly_b_in := poly_u
  
  val PolyInvNTTModule = PolyInvNTT()
  PolyInvNTTModule.io.start := poly_inv_ntt_true
  PolyInvNTTModule.io.poly_in := poly_tmp
 
  val DecompressPolyModule = DecompressPoly()
  DecompressPolyModule.io.start := decompress_poly_true
  DecompressPolyModule.io.cipher_in := decompress_ciphertext 
 
  val SubtractPolysModule = SubtractPolys()
  SubtractPolysModule.io.start := subtract_polys_true
  SubtractPolysModule.io.poly_a_in := poly_tmp
  SubtractPolysModule.io.poly_b_in := poly_v
  
  val PolynomialToMessageModule = PolynomialToMessage()
  PolynomialToMessageModule.io.start := polynomial_to_message_true
  PolynomialToMessageModule.io.poly_in := poly_tmp

  val Shake256Module = Shake256()
  Shake256Module.io.start := shake_256_true
  Shake256Module.io.state_in := message
  Shake256Module.io.length_in := 32.U
  Shake256Module.io.length_out := 32.U

  val do_algo = RegInit(false.B)
  val done = RegInit(false.B)
  val mul_1 = RegInit(false.B)
  val mul_2 = RegInit(false.B)
  val done_1 = RegInit(false.B)
  val done_2 = RegInit(false.B)

  when (io.start && !do_algo) {
    withClockAndReset(clock, reset) {
        printf("Starting Key Decaps\n")
        //printf("Cipher: 0x")
        //for (i <- 0 until 1088) {
        //  printf("%x", (io.ciphertext >> (8 * (1088 - i))) & "hFF".U)
        //}
        //printf("\n")
        printf("Secret: 0x%x\n", io.secret)
    }
    decode_pk_true_1 := true.B
    decode_pk_true_2 := true.B
    decompress_poly_true := true.B
    do_algo := true.B
    ciphertext := io.ciphertext
    decompress_ciphertext := io.ciphertext
    secret := io.secret
    //for (i <- 0 until 32) {
    //  public_seed(i) := (io.public_key >> (8 * (927 - (896 + i)))) & "hFF".U
    //}
  }

  when (decode_pk_true_1) {
    decode_pk_true_1 := false.B
  }

  when (DecodePolyModule1.io.output_valid) {
    withClockAndReset(clock, reset) {
      printf("Received decode poly message(1): 0x%x\n", Cat(DecodePolyModule1.io.poly_out))
    }
    mul_1 := true.B
    poly_s := DecodePolyModule1.io.poly_out
  }
  
  when (decode_pk_true_2) {
    decode_pk_true_2 := false.B
  }

  when (DecodePolyModule2.io.output_valid) {
    withClockAndReset(clock, reset) {
      printf("Received decode poly message(2): 0x%x\n", Cat(DecodePolyModule2.io.poly_out))
    }
    mul_2 := true.B
    poly_u := DecodePolyModule2.io.poly_out
  }

  poly_multiply_points_true := mul_1 && mul_2
  
  when (poly_multiply_points_true) {
    poly_multiply_points_true := false.B
    mul_1 := false.B
    mul_2 := false.B
  }
  when (MultiplyPolysPointsModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received multiply polys points message: 0x%x\n", Cat(MultiplyPolysPointsModule.io.poly_out))
      }
      poly_inv_ntt_true := true.B
      poly_tmp := MultiplyPolysPointsModule.io.poly_out
  }
  
  
  when (poly_inv_ntt_true) {
    poly_inv_ntt_true := false.B
  }
  when (PolyInvNTTModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received poly inv ntt message: 0x%x\n", Cat(PolyInvNTTModule.io.poly_out))
      }
      // serialize_poly_true := true.B
      // done_2 := true.B
      // poly_add_true_2 := true.B
      // add_two_in := poly_e_prime
      poly_tmp := PolyInvNTTModule.io.poly_out
      done_1 := true.B
  }
  
  when (decompress_poly_true) {
    decompress_poly_true := false.B
  }
  when (DecompressPolyModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received decompress poly message: 0x%x\n", Cat(DecompressPolyModule.io.poly_out))
      }
      // serialize_poly_true := true.B
      // done_2 := true.B
      // poly_add_true_2 := true.B
      // add_two_in := poly_e_prime
      poly_v := DecompressPolyModule.io.poly_out
      done_2 := true.B
  }

  subtract_polys_true := done_1 && done_2
  
  when (subtract_polys_true) {
    subtract_polys_true := false.B
    done_1 := false.B
    done_2 := false.B
  }
  when (SubtractPolysModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received subtract polys message: 0x%x\n", Cat(SubtractPolysModule.io.poly_out))
      }
      poly_tmp := SubtractPolysModule.io.poly_out
      polynomial_to_message_true := true.B
  }

  when (polynomial_to_message_true) {
    polynomial_to_message_true := false.B
  }
  when (PolynomialToMessageModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received poly to message message: 0x%x\n", PolynomialToMessageModule.io.message_out)
      }
      message := PolynomialToMessageModule.io.message_out
      shake_256_true := true.B
  }
  
  when (shake_256_true) {
    shake_256_true := false.B
  }

  when (Shake256Module.io.output_valid) {
    withClockAndReset(clock, reset) {
      printf("Received shake256 message: 0x%x\n", Shake256Module.io.state_out)
    }
    message := Shake256Module.io.state_out
    done := true.B
  }

  io.done := done
  // io.done := done_1 && done_2
  // io.done := true.B
}

object KeyDecaps {
  def apply(): KeyDecaps = Module(new KeyDecaps())
}

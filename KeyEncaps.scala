package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class KeyEncaps extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val public_key = Input(UInt((8*928).W))
    val cipher = Output(UInt((1088*8).W))
    val ss = Output(UInt((32*8).W))
    val done = Output(Bool())
  })

  val shake_256_true = RegInit(false.B)
  val message_to_poly_true = RegInit(false.B)
  val decode_pk_true = RegInit(false.B)
  val gen_a_true = RegInit(false.B)
  val poly_sample_true = RegInit(false.B)
  val poly_ntt_true = RegInit(false.B)
  // val poly_ntt_true_2 = RegInit(false.B)
  val poly_multiply_points_true_1 = RegInit(false.B)
  val poly_multiply_points_true_2 = RegInit(false.B)
  val poly_add_true_1 = RegInit(false.B)
  val poly_add_true_2 = RegInit(false.B)
  val poly_inv_ntt_true = RegInit(false.B)
  val encode_cipher_true = RegInit(false.B)
  val shake_256_ss_true = RegInit(false.B)
  
  val public_key = RegInit(0.U((8*928).W))
  val shake_out = RegInit(0.U(512.W))
  val public_seed = RegInit(VecInit(Seq.fill(32)(0.U(8.W))))
  val noise_seed = RegInit(0.U(256.W))
  val seed = RegInit(0.U(256.W))
  val ntt_poly_in = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val add_two_in = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_s_set = RegInit(false.B)
  val add_one_set = RegInit(false.B)
  val poly_sample_count = RegInit(0.U(2.W))
  val cipher = RegInit(0.U((1088*8).W))
  
  val poly_a = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_b = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_u = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_v = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_s = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_e = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_e_prime = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_v_prime = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  
  val Shake256Module = Shake256()
  Shake256Module.io.start := shake_256_true
  Shake256Module.io.state_in := "h3030303030303030303030303030303030303030303030303030303030303030".U
  Shake256Module.io.length_in := 32.U
  Shake256Module.io.length_out := 64.U

  val MessageToPolynomialModule = MessageToPolynomial()
  MessageToPolynomialModule.io.start := message_to_poly_true
  MessageToPolynomialModule.io.state_in := shake_out

  val DecodePolyModule = DecodePoly()
  DecodePolyModule.io.start := decode_pk_true
  DecodePolyModule.io.public_key := public_key

  val GenAModule = GenA()
  GenAModule.io.start := gen_a_true
  GenAModule.io.state_in := Cat(public_seed)

  val PolySampleModule = PolySample()
  PolySampleModule.io.start := poly_sample_true
  PolySampleModule.io.seed_in := noise_seed
  PolySampleModule.io.byte_in := poly_sample_count
  
  val PolyNTTModule = PolyNTT()
  PolyNTTModule.io.start := poly_ntt_true
  PolyNTTModule.io.poly_in := ntt_poly_in
  
  //val PolyNTTModule2 = PolyNTT()
  //PolyNTTModule2.io.start := poly_ntt_true_2
  //PolyNTTModule2.io.poly_in := poly_e
  
  val MultiplyPolysPointsModule1 = MultiplyPolysPoints()
  MultiplyPolysPointsModule1.io.start := poly_multiply_points_true_1
  MultiplyPolysPointsModule1.io.poly_a_in := poly_s
  MultiplyPolysPointsModule1.io.poly_b_in := poly_a
  
  val MultiplyPolysPointsModule2 = MultiplyPolysPoints()
  MultiplyPolysPointsModule2.io.start := poly_multiply_points_true_2
  MultiplyPolysPointsModule2.io.poly_a_in := poly_b
  MultiplyPolysPointsModule2.io.poly_b_in := poly_s
  
  val AddPolysModule1 = AddPolys()
  AddPolysModule1.io.start := poly_add_true_1
  AddPolysModule1.io.poly_a_in := poly_u
  AddPolysModule1.io.poly_b_in := poly_e
  
  val AddPolysModule2 = AddPolys()
  AddPolysModule2.io.start := poly_add_true_2
  AddPolysModule2.io.poly_a_in := poly_v_prime
  AddPolysModule2.io.poly_b_in := add_two_in
  
  val PolyInvNTTModule = PolyInvNTT()
  PolyInvNTTModule.io.start := poly_inv_ntt_true
  PolyInvNTTModule.io.poly_in := poly_v_prime
  
  val EncodeCipherModule = EncodeCipher()
  EncodeCipherModule.io.start := encode_cipher_true
  EncodeCipherModule.io.poly1_in := poly_u
  EncodeCipherModule.io.poly2_in := poly_v_prime
  
  val Shake256ModuleSS = Shake256()
  Shake256ModuleSS.io.start := shake_256_ss_true
  Shake256ModuleSS.io.state_in := seed
  Shake256ModuleSS.io.length_in := 32.U
  Shake256ModuleSS.io.length_out := 32.U

  io.ss := 0.U
  
  val do_algo = RegInit(false.B)
  val done = RegInit(false.B)
  val encode_done = RegInit(false.B)
  val ss_done = RegInit(false.B)
  val done_1 = RegInit(false.B)
  val done_2 = RegInit(false.B)

  when (io.start && !do_algo) {
    withClockAndReset(clock, reset) {
        printf("Starting Key Encaps\n")
        printf("Public Key: 0x%x\n", io.public_key)
    }
    shake_256_true := true.B
    do_algo := true.B
    public_key := io.public_key
    for (i <- 0 until 32) {
      public_seed(i) := (io.public_key >> (8 * (927 - (896 + i)))) & "hFF".U
    }
  }

  when (shake_256_true) {
    withClockAndReset(clock, reset) {
      printf("Key Gen Starting Shake 256\n")
    }
    shake_256_true := false.B
  }

  when (Shake256Module.io.output_valid) {
    withClockAndReset(clock, reset) {
      printf("Received shake256 message: 0x%x\n", Shake256Module.io.state_out)
    }
    shake_out := Shake256Module.io.state_out
    noise_seed := Shake256Module.io.state_out
    seed := Shake256Module.io.state_out >> 256
    message_to_poly_true := true.B
    shake_256_ss_true := true.B
  }

  when (message_to_poly_true) {
    message_to_poly_true := false.B
  }

  when (MessageToPolynomialModule.io.output_valid) {
    withClockAndReset(clock, reset) {
      printf("Received message to poly message: 0x%x\n", Cat(MessageToPolynomialModule.io.poly_out))
    }
    decode_pk_true := true.B
    poly_v := MessageToPolynomialModule.io.poly_out
  }

  when (decode_pk_true) {
    decode_pk_true := false.B
  }

  when (DecodePolyModule.io.output_valid) {
    withClockAndReset(clock, reset) {
      printf("Received decode poly message: 0x%x\n", Cat(DecodePolyModule.io.poly_out))
    }
    // done := true.B
    gen_a_true := true.B
    poly_b := DecodePolyModule.io.poly_out
  }
  
  when (gen_a_true) {
    gen_a_true := false.B
  }

  when (GenAModule.io.output_valid) {
    withClockAndReset(clock, reset) {
      printf("Received gen a message: 0x%x\n", Cat(GenAModule.io.state_out))
    }
    poly_sample_true := true.B
    poly_a := GenAModule.io.state_out
  }

  when (poly_sample_true) {
    withClockAndReset(clock, reset) {
      printf("poly_sample_true: noise_seed = 0x%x\n", noise_seed)
      printf("poly_sample_true: count = 0x%x\n", poly_sample_count)
    }
    poly_sample_true := false.B
  }
  when (PolySampleModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received poly sample message(%d): 0x%x\n", poly_sample_count, Cat(PolySampleModule.io.state_out))
      }

      when (poly_sample_count === 0.U) {
        poly_s := PolySampleModule.io.state_out
        poly_sample_true := true.B
      }
      .elsewhen (poly_sample_count === 1.U) {
        poly_e := PolySampleModule.io.state_out
        poly_sample_true := true.B
      }
      .otherwise{
        poly_e_prime := PolySampleModule.io.state_out
        // done := true.B
        poly_ntt_true := true.B
        // poly_ntt_true_2 := true.B
        ntt_poly_in := poly_s
      }
      poly_sample_count := poly_sample_count + 1.U
  }

  //when (poly_ntt_true_1) {
  //    poly_ntt_true_1 := false.B
  //}
  
  //when (PolyNTTModule1.io.output_valid) {
  //    withClockAndReset(clock, reset) {
  //      printf("Received poly ntt message: 0x%x\n", Cat(PolyNTTModule1.io.poly_out))
  //    }
  //    poly_s := PolyNTTModule1.io.poly_out
  //    done_1 := true.B
  //}
  
  //when (poly_ntt_true_2) {
  //    poly_ntt_true_2 := false.B
  //}
  
  //when (PolyNTTModule2.io.output_valid) {
  //    withClockAndReset(clock, reset) {
  //      printf("Received poly ntt message: 0x%x\n", Cat(PolyNTTModule2.io.poly_out))
  //    }
  //    poly_e := PolyNTTModule2.io.poly_out
  //    done_2 := true.B
  //}
  
  when (poly_ntt_true) {
      poly_ntt_true := false.B
  }
  when (PolyNTTModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received poly ntt message: 0x%x\n", Cat(PolyNTTModule.io.poly_out))
      }
      when (!poly_s_set) {
        poly_ntt_true := true.B
        ntt_poly_in := poly_e
        poly_s := PolyNTTModule.io.poly_out
        poly_s_set := true.B
      }
      .otherwise {
        // done := true.B
        poly_multiply_points_true_1 := true.B
        poly_multiply_points_true_2 := true.B
        poly_e := PolyNTTModule.io.poly_out
      }
  }
  
  when (poly_multiply_points_true_1) {
    poly_multiply_points_true_1 := false.B
  }
  when (MultiplyPolysPointsModule1.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received multiply polys points message (poly u): 0x%x\n", Cat(MultiplyPolysPointsModule1.io.poly_out))
      }
      poly_add_true_1 := true.B
      poly_u := MultiplyPolysPointsModule1.io.poly_out
  }
  
  when (poly_multiply_points_true_2) {
    poly_multiply_points_true_2 := false.B
  }
  when (MultiplyPolysPointsModule2.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received multiply polys points message (poly v prime): 0x%x\n", Cat(MultiplyPolysPointsModule2.io.poly_out))
      }
      // poly_add_true := true.B
      // done_2 := true.B
      poly_v_prime := MultiplyPolysPointsModule2.io.poly_out
      poly_inv_ntt_true := true.B
  }

  when (poly_add_true_1) {
    poly_add_true_1 := false.B
  }
  when (AddPolysModule1.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received add polys message (poly u): 0x%x\n", Cat(AddPolysModule1.io.poly_out))
      }
      // serialize_poly_true := true.B
      done_1 := true.B
      poly_u := AddPolysModule1.io.poly_out
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
      poly_add_true_2 := true.B
      add_two_in := poly_e_prime
      poly_v_prime := PolyInvNTTModule.io.poly_out
  }
  
  when (poly_add_true_2) {
    poly_add_true_2 := false.B
  }
  when (AddPolysModule2.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received add polys message (poly v prime): 0x%x\n", Cat(AddPolysModule2.io.poly_out))
      }
      when (!add_one_set) {
        poly_add_true_2 := true.B
        add_two_in := poly_v
        add_one_set := true.B
      }
      .otherwise {
        done_2 := true.B
        // encode_cipher := true.B
      }
      // serialize_poly_true := true.B
      poly_v_prime := AddPolysModule2.io.poly_out
  }

  encode_cipher_true := done_1 && done_2
  
  when (encode_cipher_true) {
    encode_cipher_true := false.B
    done_1 := false.B
    done_2 := false.B
  }
  when (EncodeCipherModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received encode cipher message: 0x")
        for (i <- 0 until 1088) {
          printf("%x", EncodeCipherModule.io.cipher_out(i))
        }
        printf("\n")
      }
      // serialize_poly_true := true.B
      // done_2 := true.B
      encode_done := true.B
      cipher := Cat(EncodeCipherModule.io.cipher_out)
  }
  
  when (shake_256_ss_true) {
    shake_256_ss_true := false.B
  }
  when (Shake256ModuleSS.io.output_valid) {
    withClockAndReset(clock, reset) {
      printf("Received shake256(ss) message: 0x%x\n", Shake256ModuleSS.io.state_out)
    }
    // done_1 := true.B
    ss_done := true.B
    io.ss := Cat(Shake256ModuleSS.io.state_out)
  }

  io.cipher := cipher
  // io.done := done
  // io.done := done_1 && done_2
  // io.done := true.B
  io.done := encode_done && ss_done
}

object KeyEncaps {
  def apply(): KeyEncaps = Module(new KeyEncaps())
}

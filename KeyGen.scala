package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class KeyGen extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val public_key = Output(UInt((8*928).W))
    val secret_key = Output(UInt((8*896).W))
    val done = Output(Bool())
  })

  val shake_256_true = RegInit(false.B)
  val gen_a_true = RegInit(false.B)
  val poly_sample_true = RegInit(false.B)
  val poly_ntt_true = RegInit(false.B)
  // val poly_sample_true_2 = RegInit(false.B)
  // val poly_ntt_true_2 = RegInit(false.B)
  val poly_multiply_points_true = RegInit(false.B)
  val poly_add_true = RegInit(false.B)
  val serialize_poly_true = RegInit(false.B)
  val serialize_public_key_true = RegInit(false.B)
  val public_seed = RegInit(0.U(256.W))
  val noise_seed = RegInit(0.U(256.W))
  val current_state = RegInit(0.U(512.W))
  val ntt_poly_in = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_s_set = RegInit(false.B)
  val poly_sample_count = RegInit(0.U(2.W))
 
  val poly_a = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_s = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_e = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_as = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_b = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  
  val secret_key = RegInit(VecInit(Seq.fill(896)(0.U(8.W))))
  val public_key = RegInit(VecInit(Seq.fill(928)(0.U(8.W))))
  val done = RegInit(false.B)
  
  val Shake256Module = Shake256()
  Shake256Module.io.start := shake_256_true
  Shake256Module.io.state_in := "h3030303030303030303030303030303030303030303030303030303030303030".U
  Shake256Module.io.length_in := 32.U
  Shake256Module.io.length_out := 64.U

  val GenAModule = GenA()
  GenAModule.io.start := gen_a_true
  GenAModule.io.state_in := public_seed
  
  val PolySampleModule = PolySample()
  PolySampleModule.io.start := poly_sample_true
  PolySampleModule.io.seed_in := noise_seed
  PolySampleModule.io.byte_in := poly_sample_count
  
  val MultiplyPolysPointsModule = MultiplyPolysPoints()
  MultiplyPolysPointsModule.io.start := poly_multiply_points_true
  MultiplyPolysPointsModule.io.poly_a_in := poly_s
  MultiplyPolysPointsModule.io.poly_b_in := poly_a
  
  val AddPolysModule = AddPolys()
  AddPolysModule.io.start := poly_add_true
  AddPolysModule.io.poly_a_in := poly_e
  AddPolysModule.io.poly_b_in := poly_as
  
  val SerializePolyModule = SerializePoly()
  SerializePolyModule.io.start := serialize_poly_true
  SerializePolyModule.io.poly_in := poly_s
  
  val SerializePublicKeyModule = SerializePublicKey()
  SerializePublicKeyModule.io.start := serialize_poly_true
  SerializePublicKeyModule.io.poly_in := poly_b
  SerializePublicKeyModule.io.seed_in := public_seed
  
  val PolyNTTModule = PolyNTT()
  PolyNTTModule.io.start := poly_ntt_true
  PolyNTTModule.io.poly_in := ntt_poly_in
  
  val got_shake = RegInit(false.B)
  val shake_out = RegInit(0.U(512.W))
 
  val do_algo = RegInit(false.B)

  when (io.start && !do_algo) {
    withClockAndReset(clock, reset) {
        printf("Starting Key Gen\n")
    }
    shake_256_true := true.B
    do_algo := true.B
  }

  when (shake_256_true) {
    withClockAndReset(clock, reset) {
      printf("Key Gen Starting Shake 256\n")
    }
    shake_256_true := false.B
  }

  when (Shake256Module.io.output_valid && !got_shake) {
    got_shake := true.B
    withClockAndReset(clock, reset) {
      printf("Received shake256 message: 0x%x\n", Shake256Module.io.state_out)
    }
    val out = Shake256Module.io.state_out
    current_state := (out)
    public_seed := out >> 256
    noise_seed := out
    shake_out := out
    gen_a_true := true.B
  }
 
  when (gen_a_true) {
    gen_a_true := false.B
    withClockAndReset(clock, reset) {
      printf("gen_a_true\n")
    }
  }
  
  when (GenAModule.io.output_valid) {
    poly_a := GenAModule.io.state_out
    withClockAndReset(clock, reset) {
      printf("Received gen a message: 0x%x\n", Cat(GenAModule.io.state_out))
    }
    poly_sample_true := true.B
  }

  when (poly_sample_true) {
    withClockAndReset(clock, reset) {
      printf("poly_sample_true: noise_seed = 0x%x\n", noise_seed)
      printf("poly_sample_true: count = 0x%x\n", poly_sample_count)
      printf("shake_out: 0x%x\n", shake_out)
    }
    poly_sample_true := false.B
  }
  when (PolySampleModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received poly sample message(%d): 0x%x\n", poly_sample_count, Cat(PolySampleModule.io.state_out))
      }

      poly_ntt_true := true.B
      ntt_poly_in := PolySampleModule.io.state_out
      when (poly_sample_count === 0.U) {
        poly_s := PolySampleModule.io.state_out
      }
      .otherwise {
        poly_e := PolySampleModule.io.state_out
      }
      poly_sample_count := poly_sample_count + 1.U
  }

  
  when (poly_ntt_true) {
      poly_ntt_true := false.B
  }
  when (PolyNTTModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received poly ntt message: 0x%x\n", Cat(PolyNTTModule.io.poly_out))
      }
      when (!poly_s_set) {
        poly_sample_true := true.B
        poly_s := PolyNTTModule.io.poly_out
        poly_s_set := true.B
      }
      .otherwise {
        poly_multiply_points_true := true.B
        poly_e := PolyNTTModule.io.poly_out
      }
  }
  
  //when (poly_sample_true_2) {
  //  withClockAndReset(clock, reset) {
  //    printf("poly_sample_true_2: noise_seed = 0x%x\n", noise_seed)
  //    printf("shake_out: 0x%x\n", shake_out)
  //  }
    // val PolySampleModule = PolySample()
    // PolySampleModule.io.start := poly_sample_true_2
    // PolySampleModule.io.seed_in := noise_seed
    // PolySampleModule.io.byte_in := 1.U
  //  when (PolySampleModule.io.output_valid) {
  //    withClockAndReset(clock, reset) {
  //      printf("Received poly sample message: 0x%x\n", Cat(PolySampleModule.io.state_out))
  //    }
  //    poly_sample_true_2 := false.B
      // poly_ntt_true_2 := true.B
  //    poly_ntt_true := true.B
  //    poly_e := PolySampleModule.io.state_out
  //    ntt_poly_in := PolySampleModule.io.state_out
  //  }
  //}

  //when (poly_ntt_true_2) {
  //  PolyNTTModule.io.start := poly_ntt_true_2
  //  PolyNTTModule.io.poly_in := poly_e
  //  when (PolyNTTModule.io.output_valid) {
  //    withClockAndReset(clock, reset) {
  //      printf("Received poly ntt message: 0x%x\n", Cat(PolyNTTModule.io.poly_out))
  //    }
  //    poly_ntt_true_2 := false.B
  //    poly_multiply_points_true := true.B
  //    poly_e := PolyNTTModule.io.poly_out
  //  }
  //}
  
  when (poly_multiply_points_true) {
    poly_multiply_points_true := false.B
  }
  when (MultiplyPolysPointsModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received multiply polys points message: 0x%x\n", Cat(MultiplyPolysPointsModule.io.poly_out))
      }
      poly_add_true := true.B
      poly_as := MultiplyPolysPointsModule.io.poly_out
  }
  
  when (poly_add_true) {
    poly_add_true := false.B
  }
  when (AddPolysModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received add polys message: 0x%x\n", Cat(AddPolysModule.io.poly_out))
      }
      serialize_poly_true := true.B
      poly_b := AddPolysModule.io.poly_out
  }

  when (serialize_poly_true) {
    serialize_poly_true := false.B
  }
  when (SerializePolyModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received serialize poly message: 0x%x\n", Cat(SerializePolyModule.io.key_out))
      }
      serialize_public_key_true := true.B
      secret_key := SerializePolyModule.io.key_out
  }

  when (serialize_public_key_true) {
    serialize_public_key_true := false.B
  }
  when (SerializePublicKeyModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("Received serialize public key message: 0x%x\n", Cat(SerializePublicKeyModule.io.key_out))
      }
      done := true.B
      public_key := SerializePublicKeyModule.io.key_out
  }


  io.public_key := Cat(public_key)
  io.secret_key := Cat(secret_key)
  io.done := done

}

object KeyGen {
  def apply(): KeyGen = Module(new KeyGen())
}

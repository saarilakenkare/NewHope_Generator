package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class MessageToPolynomial extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val state_in = Input(UInt(512.W))
    val poly_out = Output(Vec(512, UInt(16.W)))
    val output_valid = Output(Bool())
  })

  val Q = 12289
  
  val output_correct = RegInit(false.B)
  val output_index = RegInit(0.U(8.W))
  val output_reg = RegInit(0.U(1600.W))

  val state = RegInit(VecInit(Seq.fill(200)(0.U(8.W))))
  val bytes = RegInit(VecInit(Seq.fill(200)(0.U(8.W))))

  val poly_index = RegInit(0.U(8.W))
  val poly_out = Reg(Vec(512, UInt(16.W)))

  val do_algo = RegInit(false.B)
  val do_initstep = RegInit(false.B)
  val do_loop = RegInit(false.B)
  val do_loop_init = RegInit(false.B)
  val do_loop_main = RegInit(false.B)
  val do_loop_bytes_init = RegInit(false.B)
  val do_loop_while_ctr = RegInit(false.B)
  val do_loop_fill_poly = RegInit(false.B)
  val do_loop_finish = RegInit(false.B)

  val i = RegInit(0.U(8.W))
  val j = RegInit(0.U(8.W))
  val input = RegInit(0.U(512.W))

  when (io.start && !do_algo) {
    do_algo := true.B
    do_initstep := true.B
    // do_loop := true.B
    // do_loop_init := true.B
    input := io.state_in
    withClockAndReset(clock, reset) {
      printf("M2P input: 0x%x\n", io.state_in)
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
        for (j <- 0 until 8) {
          val message_i = (input >> ((63 - i) * 8)) & "hFF".U
          val v = -((message_i >> j) & 1.U)
          val mask = Wire(UInt(32.W))
          when (v != 0.U) {
            mask := "hFFFFFFFF".U
          }
          .otherwise {
            mask := 0.U
          }
          poly_out(8*i+j) := mask & (Q.U/2.U)
          poly_out(8*i+j+256) := mask & (Q.U/2.U)
          withClockAndReset(clock, reset) {
            printf("message(i): 0x%x\n", message_i)
            printf("mask: 0x%x\n", mask)
            printf("MessageToPolynomial poly_out[%d] = 0x%x\n", (8*i+j).U, mask & (Q.U / 2.U))
  
            printf("MessageToPolynomial poly_out[%d] = 0x%x\n", (8*i+j+256).U, mask & (Q.U / 2.U))
          }
        }
      }
      output_correct := true.B
    }

    /*when (do_loop) {
      
      when (do_loop_init) {
        j := 0.U
        do_loop_init := false.B
        do_loop_main := true.B
        withClockAndReset(clock, reset) {
          //printf("s: 0x%x\n", Cat(s))
          //printf("t: 0x%x\n", Cat(t))
        }
      }

      when (do_loop_main) {
        val input_i = input >> (512 - i
      }

      when (do_aloop_bytes_init) {
        withClockAndReset(clock, reset) {
          printf("aloop_bytes_init s: 0x%x\n", Cat(s))
        }
        for (i <- 0 until 25) {
          val cur = s(i)
          for (j <- 0 until 8) {
            bytes(8 * i + j) := cur >> (8 * j)
            // cur_bytes(8 * i + j) := cur >> (8 * (7 - j))
          }
        }
        do_aloop_bytes_init := false.B
        do_aloop_while_ctr := true.B

      }

      when (do_aloop_while_ctr) {
        withClockAndReset(clock, reset) {
          printf("aloop_while_ctr a: %d, bytes: 0x%x\n", a, Cat(bytes))
        }
        // val cur_bytes = Vec(Seq.fill(200)(0.U(8.W)))
        StatePermuteModule.io.start := true.B
        StatePermuteModule.io.state_in := Cat(bytes)
        do_aloop_while_ctr := false.B
      }

      when (StatePermuteModule.io.output_valid) {
          withClockAndReset(clock, reset) {
            printf("GenA/StatePermuteModule out: 0x%x\n", StatePermuteModule.io.state_out)
          }
          val spm_out = StatePermuteModule.io.state_out
          for (i <- 0 until 200) {
            bytes(i) := spm_out >> (8 * (199 - i))
          }
          poly_index := 0.U
          do_aloop_while_ctr := false.B
          do_aloop_fill_poly := true.B
          // output_correct := true.B
        
      }

      when (do_aloop_fill_poly) {
        //withClockAndReset(clock, reset) {
        //  printf("Fill Poly with Bytes: 0x%x\n", Cat(bytes))
        //}
        val value = Wire(UInt(16.W))
        value := bytes(poly_index) | (bytes(poly_index + 1.U) << 8)
        when (value < 61445.U) {
          poly_out(a * 64.U + ctr) := value
          ctr := ctr + 1.U
          withClockAndReset(clock, reset) {
            printf("poly[%d]: 0x%x\n", a * 64.U + ctr, value)
          }
        }
        poly_index := poly_index + 2.U
        when ((poly_index === (rate.U - 1.U)) || (ctr === 63.U)) {
          do_aloop_fill_poly := false.B
          do_aloop_finish := true.B
          // output_correct := true.B
        }
      }

      when (do_aloop_finish) {
        withClockAndReset(clock, reset) {
          printf("do_aloop_finish\n")
          printf("a: %d\n", a)
          printf("ctr: %d\n", ctr)
          printf("poly_index: %d\n", poly_index)
          printf("Current Poly: 0x%x\n", Cat(poly_out))
        }
        when (a === 7.U) {
          output_correct := true.B
        }
        .elsewhen (ctr >= 63.U) {
          do_aloop_init := true.B
          a := a + 1.U
        }
        .otherwise {
          do_aloop_bytes_init := true.B
        }
        do_aloop_finish := false.B
      }
    }*/
  }

  when (!output_correct) {
    io.poly_out := VecInit(Seq.fill(512)(0.U(16.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("MessageToPolynomial Done: 0x%x\n", Cat(poly_out))
    }
    output_correct := false.B
    do_algo := false.B
    io.poly_out := poly_out
    // io.state_out := Vec(Seq.fill(512)(0.U(16.W)))
    io.output_valid := true.B
  }

  // while (input_offset < io.length_in) {

  // }
  // when ((io.length_in - input_offset) < rate) {

  // }
}

object MessageToPolynomial {
  def apply(): MessageToPolynomial = Module(new MessageToPolynomial())
}

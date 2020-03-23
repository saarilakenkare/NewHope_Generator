package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class GenA extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val state_in = Input(UInt(256.W))
    val state_out = Output(Vec(512, UInt(16.W)))
    val output_valid = Output(Bool())
  })

  val output_correct = RegInit(false.B)
  val output_index = RegInit(0.U(8.W))
  val output_reg = RegInit(0.U(1600.W))

  val rate = 168

  val state = RegInit(Vec(Seq.fill(200)(0.U(8.W))))
  val bytes = RegInit(Vec(Seq.fill(200)(0.U(8.W))))

  val block_size = RegInit(0.U(16.W))
  val input_offset = RegInit(0.U(16.W))

  val do_algo = RegInit(false.B)
  val matrix = RegInit(Vec(Seq.fill(25)(0.U(64.W))))
  val round = RegInit(0.U(8.W))
  val init = RegInit(false.B)
  val R = RegInit(1.U(8.W))
  val ctr = RegInit(0.U(8.W))
  val poly_index = RegInit(0.U(8.W))
  val poly_out = Reg(Vec(512, UInt(16.W)))

  val do_initstep = RegInit(false.B)
  val do_aloop = RegInit(false.B)
  val do_aloop_init = RegInit(false.B)
  val do_aloop_for_s = RegInit(false.B)
  val do_aloop_bytes_init = RegInit(false.B)
  val do_aloop_while_ctr = RegInit(false.B)
  val do_aloop_fill_poly = RegInit(false.B)
  val do_aloop_finish = RegInit(false.B)

  val a = RegInit(0.U(4.W))
  val input = RegInit(0.U(256.W))

  val ext_seed = RegInit(Vec(Seq.fill(33)(0.U(8.W))))
  
  val StatePermuteModule = StatePermute()
  StatePermuteModule.io.start := false.B
  StatePermuteModule.io.state_in := 0.U
  
  when (io.start && !do_algo) {
    do_algo := true.B
    do_initstep := true.B
    input := io.state_in
    withClockAndReset(clock, reset) {
      printf("GenA input: 0x%x\n", io.state_in)
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
      do_initstep := false.B
      do_aloop := true.B
      do_aloop_init := true.B
    }

    when (do_aloop) {
      val s = RegInit(Vec(Seq.fill(25)(0.U(64.W))))
      val t = RegInit(Vec(Seq.fill(200)(0.U(8.W))))
      
      when (do_aloop_init) {
        ext_seed(32) := a
        ctr := 0.U
        for (i <- 0 until 33) {
          t(i) := ext_seed(i)
        }
        t(32) := a
        t(33) := "h1F".U
        t(rate - 1) := t(rate - 1) | 128.U
        do_aloop_init := false.B
        do_aloop_for_s := true.B
        withClockAndReset(clock, reset) {
          //printf("s: 0x%x\n", Cat(s))
          //printf("t: 0x%x\n", Cat(t))
        }
      }

      when (do_aloop_for_s) {
        for (i <- 0 until rate/8) {
          s(i) := Cat(t(8*i+7), t(8*i+6), t(8*i+5), t(8*i+4), t(8*i+3), t(8*i+2), t(8*i+1), t(8*i)) 
        }
        do_aloop_for_s := false.B
        do_aloop_bytes_init := true.B
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
    }
  }

  when (!output_correct) {
    io.state_out := Vec(Seq.fill(512)(0.U(16.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("Gen A Done: 0x%x\n", Cat(poly_out))
    }
    output_correct := false.B
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

object GenA {
  def apply(): GenA = Module(new GenA())
}
